package network.thunder.core.helper;

import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.RevocationHash;
import network.thunder.core.communication.layer.high.payments.PaymentData;
import network.thunder.core.communication.layer.high.payments.PaymentSecret;
import network.thunder.core.database.DBHandler;
import network.thunder.core.database.objects.ChannelSettlement;
import network.thunder.core.etc.BlockWrapper;
import network.thunder.core.etc.Constants;
import network.thunder.core.etc.Tools;
import network.thunder.core.helper.blockchain.BlockchainHelper;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.slf4j.Logger;

import java.util.List;

import static network.thunder.core.communication.layer.high.Channel.Phase.CLOSE_ON_CHAIN;
import static network.thunder.core.database.objects.ChannelSettlement.SettlementPhase.SETTLED;
import static network.thunder.core.database.objects.ChannelSettlement.SettlementPhase.UNSETTLED;

public class ChainSettlementHelper {
    private static final Logger log = Tools.getLogger();

    //Call this method whenever a block contains a transaction which spends the channel transaction
    //This method then creates the respective ChannelSettlement objects and saves them in the database
    public static void onBlock (
            BlockWrapper blockWrapper,
            DBHandler dbHandler,
            Channel channel) {

        for (Transaction transaction : blockWrapper.block.getTransactions()) {
            for (TransactionInput input : transaction.getInputs()) {
                if (input.getOutpoint().getHash().equals(channel.anchorTxHash) && input.getOutpoint().getIndex() == 0) {

                    channel.spendingTx = transaction;
                    channel.phase = CLOSE_ON_CHAIN;
                    channel.timestampForceClose = 0;
                    dbHandler.updateChannel(channel);

                    int version = ScriptTools.getVersionOutOfReturnOutput(transaction.getOutput(transaction.getOutputs().size() - 1).getScriptPubKey());
                    boolean cheated = version < channel.shaChainDepthCurrent;
                    boolean ourTx = transaction.getOutput(1).getAddressFromP2PKHScript(Constants.getNetwork()).equals(channel.addressClient);

                    RevocationHash revocationHash;
                    if (cheated && !ourTx) {
                        revocationHash = dbHandler.retrieveRevocationHash(channel.getHash(), version);
                    } else {
                        if (ourTx) {
                            revocationHash = channel.revoHashServerCurrent;
                        } else {
                            revocationHash = channel.revoHashClientCurrent;
                        }
                    }

                    if (cheated && ourTx) {
                        //TODO maybe implement claiming funds after cheating..
                        log.error("We cheated? Can'transaction claim any of these funds now..");
                        return;
                    }

                    int i = 0;
                    for (TransactionOutput output : transaction.getOutputs()) {
                        ChannelSettlement settlement = new ChannelSettlement();
                        //We can'transaction claim P2PKH outputs
                        if (output.getAddressFromP2PKHScript(Constants.getNetwork()) == null && output.getValue().value > 0) {
                            settlement.channelTx = transaction;
                            settlement.channelOutput = output;
                            settlement.channelHash = channel.getHash();
                            settlement.cheated = cheated;
                            settlement.ourChannelTx = ourTx;
                            settlement.revocationHash = revocationHash;
                            settlement.phase = UNSETTLED;
                            settlement.channelTxHeight = blockWrapper.height;
                            if (i > 1) {
                                settlement.paymentData = channel.paymentList.get(i - 2);
                                settlement.payment = true;
                            }
                            if (settlement.cheated) {
                                settlement.timeToSettle = 0; //Can claim all cheated outputs directly..
                            } else {
                                if (settlement.payment) {
                                    if (settlement.paymentData.sending) {
                                        settlement.timeToSettle = settlement.paymentData.timestampRefund;
                                    } else {
                                        settlement.timeToSettle = 0;
                                    }
                                } else {
                                    settlement.timeToSettle = settlement.channelTxHeight + channel.csvDelay;
                                }
                            }
                            dbHandler.addPaymentSettlement(settlement);
                        }
                        i++;
                    }
                }
            }
        }

    }

    //Call this method whenever a block comes in for each channel
    //This method only updates the settlement objects based on what we found in this block
    public static void onBlockSave (
            BlockWrapper blockWrapper,
            DBHandler dbHandler,
            Channel channel
    ) {
        if (channel.spendingTx != null) {
            List<ChannelSettlement> settlements = dbHandler.getSettlements(channel.getHash());
            for (ChannelSettlement settlement : settlements) {
                for (Transaction transaction : blockWrapper.block.getTransactions()) {
                    for (TransactionInput input : transaction.getInputs()) {
                        if (input.getOutpoint().equals(settlement.channelOutput.getOutPointFor())) {
                            saveSecondTx(channel, transaction, blockWrapper.height, dbHandler, settlement);
                        } else if (settlement.secondOutput != null && input.getOutpoint().equals(settlement.secondOutput.getOutPointFor())) {
                            saveThirdTx(channel, transaction, blockWrapper.height, dbHandler, settlement);
                        }
                    }
                }
            }
        }
    }

    private static void saveSecondTx (Channel channel, Transaction transaction, int blockHeight, DBHandler dbHandler, ChannelSettlement settlement) {
        settlement.secondTxHeight = blockHeight;
        settlement.secondTx = transaction;
        settlement.secondOutput = transaction.getOutput(0); //TODO for now all follow-up transactions are SISO (single-in-single-out)
        Address outputAddress = transaction.getOutput(0).getAddressFromP2PKHScript(Constants.getNetwork());
        boolean payingToUs = outputAddress != null && outputAddress.equals(channel.addressServer);

        if (!settlement.payment || (settlement.ourChannelTx && settlement.cheated)) {
            settlement.phase = SETTLED;
        } else {
            PaymentSecret secret = ScriptTools.retrievePaymentSecret(transaction.getInput(0).getScriptSig(), settlement.paymentData.secret);
            if (settlement.cheated) {
                //We only get here when the counterparty cheated
                if (payingToUs) {
                    settlement.phase = SETTLED;
                } else {
                    //The other party cheated and they try to claim the payment with the second tx..
                    if (settlement.paymentData.sending) {
                        //They must have used the secret to broadcast the second tx - maybe we still need it
                        dbHandler.addPaymentSecret(secret);
                    }
                    settlement.timeToSettle = 0;
                }
            } else {
                //Legitimate close of a payment
                if (settlement.ourChannelTx) {
                    if (settlement.paymentData.sending) {
                        if (secret == null) {
                            //our refund sec tx
                            settlement.timeToSettle = blockHeight + channel.csvDelay;
                        } else {
                            //their direct claim
                            settlement.phase = SETTLED;
                            dbHandler.addPaymentSecret(secret);
                        }
                    } else {
                        if (secret == null) {
                            //their direct refund
                            settlement.phase = SETTLED;
                        } else {
                            //our second claim
                            settlement.timeToSettle = blockHeight + channel.csvDelay;
                        }
                    }
                } else {
                    if (settlement.paymentData.sending) {
                        if (secret != null) {
                            dbHandler.addPaymentSecret(secret);
                        }
                    }
                    //If it's their channelTx, the settlement is done for us after the second tx in any case
                    settlement.phase = SETTLED;
                }
            }
        }
        dbHandler.updatePaymentSettlement(settlement);
    }

    private static void saveThirdTx (Channel channel, Transaction transaction, int blockHeight, DBHandler dbHandler, ChannelSettlement settlement) {
        settlement.thirdTxHeight = blockHeight;
        settlement.thirdTx = transaction;
        settlement.thirdOutput = transaction.getOutput(0); //TODO for now all follow-up transactions are SISO (single-in-single-out)

        //Seeing the third transaction always means this settlement is finished
        settlement.phase = SETTLED;
        dbHandler.updatePaymentSettlement(settlement);
    }

    //Call this method whenever a block comes in for each channel and for each settlement
    //This method only creates transactions and broadcasts them, based on the settlement objects
    //Cases where this method return should not happen to begin with and just exist to illustrate all possible paths
    //Should only be called for settlements where settlementTime>blockHeight
    public static void onBlockAction (
            BlockchainHelper blockchainHelper,
            Channel channel,
            ChannelSettlement settlement
    ) {
        if (!settlement.payment) {
            if (settlement.cheated) {
                if (settlement.ourChannelTx) {
                    return;
                } else {
                    claimCheatedCSVEncumbered(blockchainHelper, channel, settlement, settlement.channelOutput);
                }
            } else {
                if (settlement.ourChannelTx) {
                    if (settlement.secondTxHeight > 0) {
                        return;
                    } else {
                        claimEncumberedOutputAfterTimeout(blockchainHelper, channel, settlement, channel.csvDelay, settlement.channelOutput);
                    }
                } else {
                    return;
                }
            }
        } else {
            if (settlement.cheated) {
                if (settlement.ourChannelTx) {
                    return;
                } else {
                    if (settlement.secondTxHeight > 0) {
                        claimCheatedCSVEncumbered(blockchainHelper, channel, settlement, settlement.secondOutput);
                    } else {
                        claimCheatedPayment(blockchainHelper, channel, settlement);
                    }
                }
            } else {
                if (settlement.ourChannelTx) {
                    if (settlement.paymentData.sending) {
                        if (settlement.secondTxHeight > 0) {
                            claimEncumberedOutputAfterTimeout(blockchainHelper, channel, settlement, channel.csvDelay, settlement.secondOutput);
                        } else {
                            broadcastRefundSecondTx(blockchainHelper, channel, settlement);
                        }
                    } else {
                        if (settlement.secondTxHeight > 0) {
                            claimEncumberedOutputAfterTimeout(blockchainHelper, channel, settlement, channel.csvDelay, settlement.secondOutput);
                        } else {
                            if (settlement.paymentData.secret.secret != null) {
                                broadcastRedeemSecondTx(blockchainHelper, channel, settlement);
                            } else {
                                return;
                            }
                        }
                    }
                } else {
                    if (settlement.paymentData.sending) {
                        if (settlement.secondTxHeight > 0) {
                            return;
                        } else {
                            broadcastRefundDirectTx(blockchainHelper, channel, settlement);
                        }
                    } else {
                        if (settlement.secondTxHeight > 0) {
                            return;
                        } else {
                            if (settlement.paymentData.secret.secret != null) {
                                broadcastRedeemDirectTx(blockchainHelper, channel, settlement);
                            } else {
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    //Called when the other party broadcasted their channel tx without cheating
    // and a payment of that channel has timed out, such that we can take it now
    private static void broadcastRefundDirectTx (BlockchainHelper blockchainHelper, Channel channel, ChannelSettlement settlement) {
        broadcastDirectTx(blockchainHelper, channel, settlement, null);
    }

    //Called when the other party broadcasted their channel tx without cheating
    // and a payment of that channel belongs to us and we can claim it
    private static void broadcastRedeemDirectTx (BlockchainHelper blockchainHelper, Channel channel, ChannelSettlement settlement) {
        broadcastDirectTx(blockchainHelper, channel, settlement, settlement.paymentData.secret.secret);
    }

    private static void broadcastDirectTx (BlockchainHelper blockchainHelper, Channel channel, ChannelSettlement settlement, byte[] data) {
        Transaction claimTx = prepareTransaction(channel, settlement.channelOutput);
        Script outputScript = getOutputScript(channel, settlement);
        ScriptTools.scriptToP2SH(outputScript);
        if (settlement.paymentData.sending) {
            Tools.setTransactionLockTime(claimTx, settlement.paymentData.timestampRefund);
        }
        claimTx = addSignedInputScript(claimTx, outputScript, channel.keyServer, data);

        blockchainHelper.broadcastTransaction(claimTx);
    }

    private static void broadcastRefundSecondTx (BlockchainHelper blockchainHelper, Channel channel, ChannelSettlement settlement) {
        broadcastSecondTx(blockchainHelper, channel, settlement, null);
    }

    private static void broadcastRedeemSecondTx (BlockchainHelper blockchainHelper, Channel channel, ChannelSettlement settlement) {
        broadcastSecondTx(blockchainHelper, channel, settlement, settlement.paymentData.secret.secret);
    }

    private static void broadcastSecondTx (BlockchainHelper blockchainHelper, Channel channel, ChannelSettlement settlement, byte[] data) {

        PaymentData paymentData = settlement.paymentData;
        Script script = ScriptTools.getPaymentTxOutput(channel.keyServer, channel.keyClient, settlement.revocationHash, channel.csvDelay);
        Transaction claimTx = prepareTransaction(ScriptTools.scriptToP2SH(script), settlement.channelOutput);

        if (settlement.paymentData.sending) {
            Tools.setTransactionLockTime(claimTx, settlement.paymentData.timestampRefund);
        }

        int paymentIndex = channel.paymentList.indexOf(paymentData);

        Script outputScript = getOutputScript(channel, settlement);

        TransactionSignature signature = channel.channelSignatures.paymentSignatures.get(paymentIndex);
        TransactionSignature ourSignature = Tools.getSignature(claimTx, 0, outputScript.getProgram(), channel.keyServer);

        ScriptTools.scriptToP2SH(outputScript);

        ScriptBuilder builder = new ScriptBuilder();
        builder.data(new byte[0]);
        builder.data(ourSignature.encodeToBitcoin());
        builder.data(signature.encodeToBitcoin());
        if (data != null) {
            builder.data(data);
        } else {
            builder.data(new byte[0]);
        }
        builder.data(outputScript.getProgram());
        claimTx.getInput(0).setScriptSig(builder.build());
        blockchainHelper.broadcastTransaction(claimTx);
    }

    //Works for all output scripts that are in the form of
    //HASH EQUAL IF KEY_A ELSE CSV KEY_B END_IF CHECKSIG
    //Where the CSV timeout has been reached and we own KEY_B
    private static void claimEncumberedOutputAfterTimeout (
            BlockchainHelper blockchainHelper,
            Channel channel,
            ChannelSettlement settlement,
            int timeout,
            TransactionOutput output) {
        Transaction claimTx = prepareTransaction(channel, output);

        Script outputScript = ScriptTools.getPaymentTxOutput(channel.keyServer, channel.keyClient, settlement.revocationHash, timeout);
        TransactionSignature ourSignature = Tools.getSignature(claimTx, 0, outputScript.getProgram(), channel.keyServer);

        ScriptBuilder builder = new ScriptBuilder();
        builder.data(ourSignature.encodeToBitcoin());
        builder.data(new byte[0]);
        builder.data(outputScript.getProgram());
        claimTx.getInput(0).setScriptSig(builder.build());

        blockchainHelper.broadcastTransaction(claimTx);
    }

    //Works for all output scripts that are in the form of
    //HASH EQUAL IF KEY_A ELSE CSV KEY_B END_IF CHECKSIG
    //Where we own KEY_A and HASH
    private static void claimCheatedCSVEncumbered (
            BlockchainHelper blockchainHelper,
            Channel channel,
            ChannelSettlement settlement,
            TransactionOutput output) {
        Transaction claimTx = prepareTransaction(channel, output);

        Script outputScript = ScriptTools.getChannelTxOutputRevocation(
                settlement.revocationHash,
                channel.keyClient,
                channel.keyServer,
                channel.csvDelay);

        claimTx = addSignedInputScript(claimTx, outputScript, channel.keyServer, settlement.revocationHash.secret);
        blockchainHelper.broadcastTransaction(claimTx);
    }

    private static void claimCheatedPayment (BlockchainHelper blockchainHelper, Channel channel, ChannelSettlement settlement) {
        Transaction claimTx = prepareTransaction(channel, settlement.channelOutput);

        Script outputScript = getOutputScript(channel, settlement);
        claimTx = addSignedInputScript(claimTx, outputScript, channel.keyServer, settlement.revocationHash.secret);

        blockchainHelper.broadcastTransaction(claimTx);
    }

    private static Transaction prepareTransaction (Channel channel, TransactionOutput output) {
        Transaction claimTx = new Transaction(Constants.getNetwork());
        claimTx.addInput(output);
        claimTx.addOutput(output.getValue(), channel.addressServer);
        return claimTx;
    }

    private static Transaction prepareTransaction (Script outputScript, TransactionOutput output) {
        Transaction claimTx = new Transaction(Constants.getNetwork());
        claimTx.addInput(output);
        claimTx.getInputs().get(0).setScriptSig(Tools.getDummyScript());
        claimTx.addOutput(output.getValue(), outputScript);
        return claimTx;
    }

    private static Script getOutputScript (
            Channel channel,
            ChannelSettlement settlement) {
        ECKey revocableKey;
        ECKey counterpartyKey;
        PaymentData paymentData = settlement.paymentData;

        if (settlement.ourChannelTx) {
            revocableKey = channel.keyServer;
            counterpartyKey = channel.keyClient;
        } else {
            revocableKey = channel.keyClient;
            counterpartyKey = channel.keyServer;
        }

        if (paymentData != null) {
            if (settlement.ourChannelTx) {
                return ScriptTools.getChannelTxOutputPayment(channel, settlement.paymentData, settlement.revocationHash);
            } else {
                return ScriptTools.getChannelTxOutputPayment(channel.reverse(), settlement.paymentData.reverse(), settlement.revocationHash);
            }
        } else {
            return ScriptTools.getChannelTxOutputRevocation(settlement.revocationHash, revocableKey, counterpartyKey, channel.csvDelay);
        }

    }

    private static Transaction addSignedInputScript (Transaction transaction, Script outputScript, ECKey key, byte[] data) {
        ScriptBuilder builder = new ScriptBuilder();

        TransactionSignature ourSignature = Tools.getSignature(transaction, 0, outputScript.getProgram(), key);

        builder.data(ourSignature.encodeToBitcoin());
        if (data != null) {
            builder.data(data);
        } else {
            builder.data(new byte[0]);
        }
        builder.data(outputScript.getProgram());

        transaction.getInput(0).setScriptSig(builder.build());

        Sha256Hash h = transaction.hashForSignature(0, outputScript, Transaction.SigHash.ALL, false);

        return transaction;
    }

}
