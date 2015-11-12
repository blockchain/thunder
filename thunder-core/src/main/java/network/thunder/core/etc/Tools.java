/*
 * ThunderNetwork - Server Client Architecture to send Off-Chain Bitcoin Payments
 * Copyright (C) 2015 Mats Jerratsch <matsjj@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package network.thunder.core.etc;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import com.sun.org.apache.xml.internal.security.utils.Base64;
import org.bitcoinj.core.*;
import org.bitcoinj.core.ECKey.ECDSASignature;
import org.bitcoinj.core.Transaction.SigHash;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicHierarchy;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.spongycastle.crypto.digests.RIPEMD160Digest;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The Class Tools.
 */
public class Tools {

    /**
     * The Constant hexArray.
     */
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    /**
     * Input stream to string.
     *
     * @param in the in
     * @return the string
     */
    public static String InputStreamToString (InputStream in) {
        String qry = "";
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte buf[] = new byte[4096];
            for (int n = in.read(buf); n > 0; n = in.read(buf)) {
                out.write(buf, 0, n);
            }
            qry = new String(out.toByteArray());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try {
            return java.net.URLDecoder.decode(qry, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return qry;
    }

    public static boolean arrayListContainsByteArray (ArrayList<byte[]> arrayList, byte[] bytes) {
        //TODO: This is a probably slow hack, better way would be to use a helper class, as we can make use of hashCode then..
        /* Example wrapper class to have more efficient contains(..)
        public final class Bytes { private final int hashCode; private final byte[] data; public Bytes(byte[] in) { this.data = in; this.hashCode = Arrays
        .hashCode(in); } @Override public boolean equals(Object other) {if (other == null) return false; if (!(other instanceof Bytes)) return false; if ((
        (Bytes) other).hashCode != hashCode) return false; return Arrays.equals(data, ((Bytes) other).data); } @Override public int hashCode()
        {return hashCode;} @Override public String toString() { ... do something useful here ... }}
         */
        for (byte[] a : arrayList) {
            if (Arrays.equals(a, bytes)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Bool to int.
     *
     * @param bool the bool
     * @return the int
     */
    public static int boolToInt (boolean bool) {
        int a = 0;
        if (bool) {
            a = 1;
        }
        return a;
    }

    /**
     * Byte to string.
     *
     * @param array the array
     * @return the string
     */
    //	http://java-performance.info/base64-encoding-and-decoding-performance/
    public static String byteToString (byte[] array) {
        return new String(Base64.encode(array));
    }

    /**
     * Byte to string58.
     *
     * @param array the array
     * @return the string
     */
    public static String byteToString58 (byte[] array) {
        return Base58.encode(array);
    }

    /**
     * Bytes to hex.
     *
     * @param bytes the bytes
     * @return the string
     */
    public static String bytesToHex (byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Calculate server fee.
     *
     * @param amount the amount
     * @return the long
     */
    public static long calculateServerFee (long amount) {
        long fee = (long) (amount * Constants.SERVER_FEE_PERCENTAGE + Constants.SERVER_FEE_FLAT);
        return Math.min(Constants.SERVER_FEE_MAX, Math.max(Constants.SERVER_FEE_MIN, fee));
    }

    /**
     * Calculate server fee reverse.
     *
     * @param amount the amount
     * @return the long
     */
    public static long calculateServerFeeReverse (long amount) {
        long a = Constants.SERVER_FEE_FLAT;
        long b = amount + a;
        long c = (long) ((b / (1 - Constants.SERVER_FEE_PERCENTAGE)) - b);
        return Math.min(Constants.SERVER_FEE_MAX, Math.max(Constants.SERVER_FEE_MIN, c + a));
    }

    /**
     * Check server fee.
     *
     * @param amountIs       the amount is
     * @param amountShouldBe the amount should be
     * @return true, if successful
     */
    public static boolean checkServerFee (long amountIs, long amountShouldBe) {
        long fee = amountShouldBe - amountIs;
        long feeShouldBe = calculateServerFee(amountShouldBe);
        return (fee <= feeShouldBe);
    }

    /**
     * Check signature.
     *
     * @param transaction   the transaction
     * @param index         the index
     * @param outputToSpend the output to spend
     * @param key           the key
     * @param signature     the signature
     * @return true, if successful
     */
    public static boolean checkSignature (Transaction transaction, int index, TransactionOutput outputToSpend, ECKey key, byte[] signature) {
        Sha256Hash hash = transaction.hashForSignature(index, outputToSpend.getScriptBytes(), SigHash.ALL, false);
        return key.verify(hash, ECDSASignature.decodeFromDER(signature));
    }

    /**
     * Check transaction fees.
     *
     * @param size        the size
     * @param transaction the transaction
     * @param output      the output
     * @return true, if successful
     */
    public static boolean checkTransactionFees (int size, Transaction transaction, TransactionOutput output) {
        long in = output.getValue().value;
        long out = 0;
        for (TransactionOutput o : transaction.getOutputs()) {
            out += o.getValue().value;
        }
        long diff = in - out;
        float f = ((float) diff) / size;

        if (f >= Constants.FEE_PER_BYTE_MIN) {
            if (f <= Constants.FEE_PER_BYTE_MAX) {
                return true;
            }
        }
        System.out.println("Fee not correct. Total Fee: " + diff + " Per Byte: " + f + " Size: " + size);
        return false;
    }

    /**
     * Check transaction lock time.
     *
     * @param transaction the transaction
     * @param locktime    the locktime
     * @return true, if successful
     */
    public static boolean checkTransactionLockTime (Transaction transaction, int locktime) {
        if (Math.abs(transaction.getLockTime() - locktime) > 5 * 60) {
            System.out.println("Locktime not correct. Should be: " + locktime + " Is: " + transaction.getLockTime() + " Diff: " + Math.abs(transaction
                    .getLockTime() - locktime));
            return false;
        }

        if (locktime == 0) {
            return true;
        }

        for (TransactionInput input : transaction.getInputs()) {
            if (input.getSequenceNumber() == 0) {
                return true;
            }
        }
        System.out.println("No Sequence Number is 0..");
        return false;
    }

    /**
     * Compare hash.
     *
     * @param hash1 the hash1
     * @param hash2 the hash2
     * @return true, if successful
     */
    public static boolean compareHash (Sha256Hash hash1, Sha256Hash hash2) {
        return hash1.toString().equals(hash2.toString());
    }

    //	public static void emailException (Exception e, Message m, Channel c, Payment p, Transaction channelTransaction, Transaction t) throws
    // AddressException {
    //		String to = "matsjj@gmail.com";
    //		String from = "exception@thunder.network";
    //		String host = "localhost";
    //		Properties properties = System.getProperties();
    //		properties.setProperty("mail.smtp.host", host);
    //		Session session = Session.getDefaultInstance(properties);

    /**
     * Current time.
     *
     * @return the int
     */
    public static int currentTime () {
        return ((int) (System.currentTimeMillis() / 1000));
    }
    //		try {
    //			MimeMessage message = new MimeMessage(session);
    //			message.setFrom(new InternetAddress(from));
    //			message.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));

    /**
     * Gets the coin value from input.
     *
     * @param peer  the peer
     * @param input the input
     * @return the coin value from input
     * @throws InterruptedException the interrupted exception
     * @throws ExecutionException   the execution exception
     * @throws TimeoutException     the timeout exception
     */
    public static long getCoinValueFromInput (Peer peer, List<TransactionInput> input) throws InterruptedException, ExecutionException, TimeoutException {
        List<TransactionOutPoint> outpointList = new ArrayList<TransactionOutPoint>();
        for (TransactionInput t : input) {
            outpointList.add(t.getOutpoint());
        }
        UTXOsMessage m = peer.getUTXOs(outpointList).get(100, TimeUnit.SECONDS);

        long value = 0;
        for (TransactionOutput out : m.getOutputs()) {
            value += out.getValue().value;
        }
        System.out.println(m);
        if (outpointList.size() != m.getOutputs().size()) {
            boolean found = false;
            for (TransactionOutPoint o : outpointList) {
                //				for(TransactionOutput u : m.getOutputs()) {
                //					if(o.getHash().toString().equals(u.getParentTransactionHash().toString())) {
                //						found = true;
                //						break;
                //					}
                //				}
                //				if(found) break;
                System.out.println("Transaction Input not found in UTXO: " + o.getHash() + " " + o.getIndex());
            }
        }

        return value;

    }
    //			message.setSubject("New Critical Exception thrown..");

    /**
     * Gets the coin value from output.
     *
     * @param output the output
     * @return the coin value from output
     */
    public static long getCoinValueFromOutput (List<TransactionOutput> output) {
        long sumOutputs = 0;
        for (TransactionOutput out : output) {
            sumOutputs += out.getValue().value;
        }
        return sumOutputs;
    }
    //			String text = "";

    /**
     * Gets the dummy script.
     *
     * @return the dummy script
     */
    public static Script getDummyScript () {
        ScriptBuilder builder = new ScriptBuilder();
        builder.smallNum(0);
        return builder.build();
    }
    //			text += Tools.stacktraceToString(e);
    //			text += "\n";
    //			if (m != null) {
    //				text += m;
    //			}
    //			text += "\n";
    //			if (c != null) {
    //				text += c;
    //			}
    //			text += "\n";
    //			if (p != null) {
    //				text += p;
    //			}
    //			text += "\n";
    //			if (channelTransaction != null) {
    //				text += channelTransaction;
    //			}
    //			text += "\n";
    //			if (t != null) {
    //				text += t;
    //			}

    /**
     * Gets the four character hash.
     *
     * @param s the s
     * @return the four character hash
     * @throws NoSuchAlgorithmException the no such algorithm exception
     */
    public static String getFourCharacterHash (String s) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(s.getBytes());
        String encryptedString = Tools.byteToString58(messageDigest.digest());

        return encryptedString.substring(0, 3);
    }
    //			// Now set the actual message
    //			message.setText(text);

    /**
     * Call to get the MasterKey for a new Channel.
     * TODO: Change to request master node key..
     *
     * @param number Query the Database to get the latest unused number
     * @return DeterministicKey for the new Channel
     */
    public static DeterministicKey getMasterKey (int number) {

        DeterministicKey hd = DeterministicKey.deserializeB58(SideConstants.KEY_B58, Constants.getNetwork());
        //		DeterministicKey hd =  DeterministicKey.deserializeB58(null,KEY_B58);
        //        DeterministicKey hd = HDKeyDerivation.createMasterPrivateKey(KEY.getBytes());
        DeterministicHierarchy hi = new DeterministicHierarchy(hd);

        List<ChildNumber> childList = new ArrayList<ChildNumber>();
        ChildNumber childNumber = new ChildNumber(number, true);
        childList.add(childNumber);

        DeterministicKey key = hi.get(childList, true, true);
        return key;

    }
    //			// Send message
    //			Transport.send(message);
    //			System.out.println("Sent message successfully....");
    //		} catch (javax.mail.MessagingException e1) {
    //		}
    //	}

    //	/**
    //	 * Gets the channel refund transaction.
    //	 *
    //	 * @param channel the channel
    //	 * @return the channel refund transaction
    //	 * @throws SQLException           the SQL exception
    //	 * @throws AddressFormatException the address format exception
    //	 */
    //	public static TransactionWrapper getChannelRefundTransaction (Channel channel) throws SQLException, AddressFormatException {
    //		Transaction refundTransaction = new Transaction(Constants.getNetwork());

    public static Message getMessage (String data) throws IOException {
        data = java.net.URLDecoder.decode(data, "UTF-8");
        Gson gson = new Gson();
        Message message = gson.fromJson(data, Message.class);
        return message;
    }
    //		refundTransaction.addOutput(Coin.valueOf(channel.getInitialAmountClient() - Tools.getTransactionFees(1, 2)), channel
    // .getChangeAddressClientAsAddress
    //				());
    //		refundTransaction.addOutput(Coin.valueOf(channel.getInitialAmountServer()), new Address(Constants.getNetwork(), channel.getChangeAddressServer()));

    /**
     * Gets the multisig input script.
     *
     * @param client the client
     * @param server the server
     * @return the multisig input script
     */
    public static Script getMultisigInputScript (ECDSASignature client, ECDSASignature server) {
        ArrayList<TransactionSignature> signList = new ArrayList<TransactionSignature>();
        signList.add(new TransactionSignature(client, SigHash.ALL, false));
        signList.add(new TransactionSignature(server, SigHash.ALL, false));
        Script inputScript = ScriptBuilder.createMultiSigInputScript(signList);
        /*
         * Seems there is a bug here,
		 * https://groups.google.com/forum/#!topic/bitcoinj/A9R8TdUsXms
		 */
        Script workaround = new Script(inputScript.getProgram());
        return workaround;

    }
    //		refundTransaction.setLockTime(channel.getTimestampClose());

    public static byte[] getRandomByte (int amount) {
        byte[] b = new byte[amount];
        Random r = new Random();
        r.nextBytes(b);
        return b;
    }
    //		refundTransaction.addInput(channel.getOpeningTx().getOutput(0));
    //		refundTransaction.getInput(0).setSequenceNumber(0);

    public static TransactionSignature getSignature (Transaction transactionToSign, int index, TransactionOutput outputToSpend, ECKey key) {
        return getSignature(transactionToSign, index, outputToSpend.getScriptBytes(), key);
    }

    public static TransactionSignature getSignature (Transaction transactionToSign, int index, byte[] outputToSpend, ECKey key) {
        Sha256Hash hash = transactionToSign.hashForSignature(index, outputToSpend, SigHash.ALL, false);

        ECDSASignature signature = key.sign(hash);
        return new TransactionSignature(signature, SigHash.ALL, false);
    }
    //		Sha256Hash sighash = refundTransaction.hashForSignature(0, channel.getOpeningTx().getOutput(0).getScriptPubKey(), SigHash.ALL, false);
    //		ECDSASignature signature = channel.getServerKeyOnServer().sign(sighash);

    /**
     * Gets the transaction fees.
     *
     * @param size the size
     * @return the transaction fees
     */
    public static long getTransactionFees (int size) {
        //		return (Math.ceil( ( (float) size )/1000) ) * 1000 * 500 ;
        return (long) (size * Constants.FEE_PER_BYTE);
    }
    //		return new TransactionWrapper(refundTransaction, signature);
    //	}
    //TODO: These 2 methods are pretty slow, change them to Base64 in production:

    /**
     * With reference to
     * http://bitcoin.stackexchange.com/questions/1195/how-to-calculate-transaction-size-before-sending
     *
     * @param inputs  the inputs
     * @param outputs the outputs
     * @return the transaction fees
     */
    public static long getTransactionFees (int inputs, int outputs) {
        /*
         * One output will pay to multi sig, 144 bytes
		 */
        int size = inputs * 180 + (outputs - 1) * 34 + 144 + 10 + 40;
        return Tools.getTransactionFees(size);
    }

    public static byte[] hashSecret (byte[] secret) {

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            md.update(secret);
            byte[] digest = md.digest();

            RIPEMD160Digest dig = new RIPEMD160Digest();
            dig.update(digest, 0, digest.length);

            byte[] out = new byte[20];
            dig.doFinal(out, 0);

            return out;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static Transaction addOutAndInputs (long value, Wallet wallet, HashMap<TransactionOutPoint, Integer> lockedOutputs, Script script) {
        final int TIME_LOCK_IN_SECONDS = 60;

        Transaction transaction = new Transaction(Constants.getNetwork());

        long totalInput = 0;
        long neededAmount = value + Tools.getTransactionFees(20, 2);
        ArrayList<TransactionOutput> outputList = new ArrayList<>();
        ArrayList<TransactionOutput> spendable = new ArrayList<>(wallet.calculateAllSpendCandidates());
        ArrayList<TransactionOutput> tempList = new ArrayList<>(spendable);
        for (TransactionOutput o : tempList) {
            int timeLock = lockedOutputs.get(o);
            if (timeLock > Tools.currentTime()) {
                spendable.remove(o);
            }
        }
        for (TransactionOutput o : wallet.calculateAllSpendCandidates()) {
            if (o.getValue().value > neededAmount) {
                /*
                 * Ok, found a suitable output, need to split the change
                 * TODO: Change (a few things), such that there will be no output < 500...
                 */
                outputList.add(o);
                totalInput += o.getValue().value;

            }
        }
        if (totalInput == 0) {
            /*
             * None of our outputs alone is sufficient, have to add multiples..
             */
            for (TransactionOutput o : wallet.calculateAllSpendCandidates()) {
                if (totalInput >= neededAmount) {
                    continue;
                }
                totalInput += o.getValue().value;
                outputList.add(o);
            }
        }

        if (totalInput < value) {
            /*
             * Not enough outputs in total to pay for the channel..
             */
            throw new RuntimeException("Wallet Balance not sufficient"); //TODO
        } else {

            transaction.addOutput(Coin.valueOf(value), script);
            transaction.addOutput(Coin.valueOf(totalInput - value - Tools.getTransactionFees(2, 2)), wallet.freshReceiveAddress());

            for (TransactionOutput o : outputList) {
                transaction.addInput(o);
            }

            /*
             * Sign all of our inputs..
             */
            int j = 0;
            for (int i = 0; i < outputList.size(); i++) {
                TransactionOutput o = outputList.get(i);
                transaction.getInput(i).setScriptSig(new Script(Tools.getSignature(transaction, i, o, wallet.findKeyFromPubHash(o.getAddressFromP2PKHScript
                        (Constants.getNetwork()).getHash160())).encodeToDER()));
                //TODO: Currently only working if we have P2PKH outputs in our wallet
            }
        }

        return transaction;
    }

    /**
     * Hash.java secret.
     *
     * @param secret the secret
     * @return the string
     * @throws UnsupportedEncodingException the unsupported encoding exception
     * @throws NoSuchAlgorithmException     the no such algorithm exception
     */
    public static String hashSecretToString (byte[] secret) {

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            md.update(secret);
            byte[] digest = md.digest();

            RIPEMD160Digest dig = new RIPEMD160Digest();
            dig.update(digest, 0, digest.length);

            byte[] out = new byte[20];
            dig.doFinal(out, 0);

            return Tools.byteToString(out);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] hashSha (byte[] secret) {

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            md.update(secret);
            byte[] digest = md.digest();

            return digest;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] hashSha (byte[] secret, int rounds) {

        try {
            byte[] digest = secret;
            for (int i = 0; i < rounds; i++) {
                MessageDigest md = MessageDigest.getInstance("SHA-256");

                md.update(digest);
                digest = md.digest();
            }
            return digest;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] hexStringToByteArray (String s) {
        s = s.replaceAll(" ", "").toLowerCase();
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Int to bool.
     *
     * @param i the i
     * @return true, if successful
     */
    public static boolean intToBool (int i) {
        boolean a = true;
        if (i == 0) {
            a = false;
        }
        return a;
    }

    public static byte[] intToByte (int i) {
        //TODO: Add functionality..

        return new byte[4];
    }

    /**
     * Send message.
     *
     * @param httpExchange the http exchange
     * @param message      the message
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void sendMessage (HttpExchange httpExchange, Message message) throws IOException {
        String response = new Gson().toJson(message);
        httpExchange.sendResponseHeaders(200, response.length());
        OutputStream os = httpExchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    /**
     * Sets the transaction lock time.
     *
     * @param transaction the transaction
     * @param locktime    the locktime
     * @return the transaction
     */
    public static Transaction setTransactionLockTime (Transaction transaction, int locktime) {
        transaction.setLockTime(locktime);
        for (TransactionInput input : transaction.getInputs()) {
            input.setSequenceNumber(0);
        }
        return transaction;
    }

    /**
     * Stacktrace to string.
     *
     * @param e the e
     * @return the string
     */
    public static String stacktraceToString (Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * String to byte.
     *
     * @param string the string
     * @return the byte[]
     */
    public static byte[] stringToByte (String string) {
        try {
            return Base64.decode(string);
        } catch (Base64DecodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * String to byte58.
     *
     * @param string the string
     * @return the byte[]
     */
    public static byte[] stringToByte58 (String string) {
        try {
            return Base58.decode(string);
        } catch (AddressFormatException e) {
            throw new RuntimeException(e);
        }
    }

    //
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //

}
