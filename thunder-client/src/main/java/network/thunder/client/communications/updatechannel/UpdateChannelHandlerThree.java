/*
 *  ThunderNetwork - Server Client Architecture to send Off-Chain Bitcoin Payments
 *  Copyright (C) 2015 Mats Jerratsch <matsjj@gmail.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *  
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package network.thunder.client.communications.updatechannel;

import network.thunder.client.communications.objects.UpdateChannelRequestThree;
import network.thunder.client.communications.objects.UpdateChannelResponseThree;
import network.thunder.client.database.MySQLConnection;
import network.thunder.client.database.objects.Channel;
import network.thunder.client.database.objects.KeyWrapper;
import network.thunder.client.database.objects.Payment;
import network.thunder.client.etc.Constants;
import network.thunder.client.etc.ScriptTools;
import network.thunder.client.etc.Tools;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey.ECDSASignature;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.script.Script;

import java.sql.Connection;
import java.util.ArrayList;

public class UpdateChannelHandlerThree {
    public Connection conn;
    public Channel channel;

    /**
     * List of all payments in the new channel
     */
    public ArrayList<Payment> newPaymentsTotal = new ArrayList<Payment>();

    public Sha256Hash serverHash;

    public void evaluate (UpdateChannelResponseThree m) throws Exception {

        serverHash = new Sha256Hash(Tools.stringToByte(m.transactionHash));

    }

    public UpdateChannelRequestThree request () throws Exception {
        UpdateChannelRequestThree request = new UpdateChannelRequestThree();

        KeyWrapper keyWrapper = MySQLConnection.getKeysPubOnly(conn, channel, false);

        Transaction clientTransaction = channel.getChannelTxClientTemp();
        Transaction channelTransaction = new Transaction(Constants.getNetwork());

        channelTransaction.addOutput(Coin.valueOf(0), channel.getChangeAddressClientAsAddress());
        channelTransaction.addOutput(Coin.valueOf(clientTransaction.getOutput(1).getValue().value + Tools.getTransactionFees(Constants.SIZE_REVOKE_TX)), ScriptTools.getRevokeScript(keyWrapper, channel, Constants.SERVERSIDE));

        ArrayList<Payment> paymentListOrdered = new ArrayList<Payment>();
        /**
         * We trust the transaction we received from the server here, especially since we have
         * 		validated all payments one step earlier.
         *
         * As a side-effect we also have the list ordered afterwards.
         */
        for (int i = 2; i < clientTransaction.getOutputs().size(); ++i) {
            TransactionOutput output = clientTransaction.getOutputs().get(i);

            String secretHash = ScriptTools.getRofPaymentScript(output);
            Payment payment = Tools.getPaymentOutOfList(newPaymentsTotal, secretHash);
            paymentListOrdered.add(payment);

            Script outputScript = ScriptTools.getPaymentScript(channel, keyWrapper, payment.getSecretHash(), Constants.SERVERSIDE, payment.paymentToServer);
            channelTransaction.addOutput(output.getValue(), outputScript);
        }
        newPaymentsTotal = paymentListOrdered;

        /**
         * Set those keys from the list as used, that have been around in any of the Scripts.
         */
        MySQLConnection.setKeysUsed(conn, keyWrapper);

        channelTransaction.addInput(new Sha256Hash(channel.getOpeningTxHash()), 0, Tools.getDummyScript());

        /**
         * Calculate the fees and set the client change accordingly.
         */
        int size = channelTransaction.getMessageSize() + 72 * 2;
        long sumOutputs = Tools.getCoinValueFromOutput(channelTransaction.getOutputs());
        long sumInputs = channel.getOpeningTx().getOutput(0).getValue().value;
        channelTransaction.getOutput(0).setValue(Coin.valueOf(sumInputs - sumOutputs - Tools.getTransactionFees(size)));

        /**
         * Add our signature.
         */
        ECDSASignature clientSig = Tools.getSignature(channelTransaction, 0, channel.getOpeningTx().getOutput(0), channel.getClientKeyOnClient());
        channelTransaction.getInput(0).setScriptSig(ScriptTools.getMultisigInputScript(clientSig));

        request.channelTransaction = Tools.byteToString(channelTransaction.bitcoinSerialize());

        channel.setChannelTxServerTemp(channelTransaction);
        return request;
    }
}
