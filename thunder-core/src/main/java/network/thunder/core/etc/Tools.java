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
import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import com.sun.org.apache.xml.internal.security.utils.Base64;
import org.bitcoinj.core.*;
import org.bitcoinj.core.ECKey.ECDSASignature;
import org.bitcoinj.core.Transaction.SigHash;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.spongycastle.crypto.digests.RIPEMD160Digest;

import java.io.*;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * The Class Tools.
 */
public class Tools {

    final protected static char[] hexArray = "0123456789abcdef".toCharArray();

    public static int getRandom (int min, int max) {
        return new Random().nextInt(max + 1 - min) + min;
    }

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

    public static <T extends Object> T getRandomItemFromList (List<T> list) {
        System.out.println(list.size());
        int randomNumber = new Random().nextInt(list.size());
        return list.get(randomNumber);
    }

    public static <T> List<T> getRandomSubList (List<T> input, int subsetSize) {
        Random r = new Random();
        int inputSize = input.size();
        for (int i = 0; i < subsetSize; i++) {
            int indexToSwap = i + r.nextInt(inputSize - i);
            T temp = input.get(i);
            input.set(i, input.get(indexToSwap));
            input.set(indexToSwap, temp);
        }
        return input.subList(0, subsetSize);
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

    public static int currentTime () {
        return ((int) (System.currentTimeMillis() / 1000));
    }

    public static int currentTimeFlooredToCurrentDay () {
        int time = currentTime();
        int diff = time % 86400;
        return time - diff;
    }

    public static long getCoinValueFromOutput (List<TransactionOutput> output) {
        long sumOutputs = 0;
        for (TransactionOutput out : output) {
            sumOutputs += out.getValue().value;
        }
        return sumOutputs;
    }

    public static Script getDummyScript () {
        ScriptBuilder builder = new ScriptBuilder();
        builder.smallNum(0);
        return builder.build();
    }

    public static String getFourCharacterHash (String s) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(s.getBytes());
        String encryptedString = Tools.byteToString58(messageDigest.digest());

        return encryptedString.substring(0, 3);
    }

    public static Message getMessage (String data) throws IOException {
        data = java.net.URLDecoder.decode(data, "UTF-8");
        Gson gson = new Gson();
        Message message = gson.fromJson(data, Message.class);
        return message;
    }

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

    public static byte[] getRandomByte (int amount) {
        byte[] b = new byte[amount];
        Random r = new Random();
        r.nextBytes(b);
        return b;
    }

    public static List<byte[]> byteBufferListToByteArrayList (List<ByteBuffer> byteBufferList) {
        List<byte[]> byteArrayList = new ArrayList<>(byteBufferList.size());
        for (ByteBuffer b : byteBufferList) {
            byteArrayList.add(b.array());
        }
        return byteArrayList;
    }

    public static byte[] copyRandomByteInByteArray (byte[] dest, int offset, int length) {
        byte[] error = getRandomByte(length);
        System.arraycopy(error, 0, dest, offset, length);
        return dest;
    }

    public static TransactionSignature getSignature (Transaction transactionToSign, int index, TransactionOutput outputToSpend, ECKey key) {
        return getSignature(transactionToSign, index, outputToSpend.getScriptBytes(), key);
    }

    public static TransactionSignature getSignature (Transaction transactionToSign, int index, byte[] outputToSpend, ECKey key) {
        Sha256Hash hash = transactionToSign.hashForSignature(index, outputToSpend, SigHash.ALL, false);
        ECDSASignature signature = key.sign(hash).toCanonicalised();
        return new TransactionSignature(signature, SigHash.ALL, false);
    }

    public static long getTransactionFees (int size) {
        return (long) (size * Constants.FEE_PER_BYTE);
    }

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

    public static Transaction setTransactionLockTime (Transaction transaction, int locktime) {
        transaction.setLockTime(locktime);
        for (TransactionInput input : transaction.getInputs()) {
            input.setSequenceNumber(0);
        }
        return transaction;
    }

    public static String stacktraceToString (Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    public static byte[] stringToByte (String string) {
        try {
            com.sun.org.apache.xml.internal.security.Init.init();
            return Base64.decode(string);
        } catch (Base64DecodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] stringToByte58 (String string) {
        try {
            return Base58.decode(string);
        } catch (AddressFormatException e) {
            throw new RuntimeException(e);
        }
    }
}
