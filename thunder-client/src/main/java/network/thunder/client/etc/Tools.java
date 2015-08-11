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
package network.thunder.client.etc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import network.thunder.client.communications.Message;
import network.thunder.client.database.objects.Channel;
import network.thunder.client.database.objects.Payment;
import network.thunder.client.database.objects.TransactionWrapper;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.ECKey.ECDSASignature;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Transaction.SigHash;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.UTXOsMessage;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.spongycastle.crypto.digests.RIPEMD160Digest;

import com.google.gson.Gson;
import com.subgraph.orchid.encoders.Base64;

public class Tools {
	public static int currentTime() {
		return ((int) (System.currentTimeMillis()/1000));
	}
	
	public static String InputStreamToString(InputStream in) {
		String qry="";
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
	
	public static Payment getPaymentOutOfList(ArrayList<Payment> paymentList, String secretHash) {
		for(Payment p : paymentList) {
			if(p.getSecretHash().equals(secretHash))
				return p;
		}
		return null;
	}
	
	
	public static long calculateServerFee(long amount) {
		long fee = (long) (amount * Constants.SERVER_FEE_PERCENTAGE + Constants.SERVER_FEE_FLAT);
		return Math.min(Constants.SERVER_FEE_MAX, Math.max(Constants.SERVER_FEE_MIN, fee));
	}
	
	public static long calculateServerFeeReverse(long amount) {
		long a = Constants.SERVER_FEE_FLAT;
		long b = amount + a;
		long c = (long) ((b / (1-Constants.SERVER_FEE_PERCENTAGE)) - b);
		return Math.min(Constants.SERVER_FEE_MAX, Math.max(Constants.SERVER_FEE_MIN, c+a));
	}
	
	public static int checkServerFee(long amountIs, long amountShouldBe) {
		long fee = amountShouldBe - amountIs;
		long feeShouldBe = calculateServerFee(amountShouldBe);
		if(fee==feeShouldBe) return 0;
		if(fee>feeShouldBe) return -1;
		if(fee<feeShouldBe) return 1;
		return -1;
	}
	
	public static String getFourCharacterHash(String s) throws NoSuchAlgorithmException {
		MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
		messageDigest.update(s.getBytes());
		String encryptedString = Tools.byteToString58(messageDigest.digest());
		
		return encryptedString.substring(0, 4);
	}

	
	public static String stacktraceToString(Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		return sw.toString();
	}
	
	public static int boolToInt(boolean bool) {
	    int a = 0;
	    if(bool) a=1;
	    return a;
	}
	
	public static boolean intToBool(int i) {
	    boolean a=true;
	    if(i == 0) a = false;
	    return a;
	}
	
	public static long getCoinValueFromInput(Peer peer, List<TransactionInput> input) throws InterruptedException, ExecutionException, TimeoutException {
		List<TransactionOutPoint> outpointList = new ArrayList<TransactionOutPoint>();
		for(TransactionInput t : input) {
			outpointList.add(t.getOutpoint());
		}
		UTXOsMessage m = peer.getUTXOs(outpointList).get(100, TimeUnit.SECONDS);
		
		long value = 0;
		for(TransactionOutput out : m.getOutputs() ){
			value+=out.getValue().value;
		}

		return value;

	}
	
	public static long getCoinValueFromOutput(List<TransactionOutput> output) {
		long sumOutputs = 0;
		for(TransactionOutput out : output) {
			sumOutputs += out.getValue().value;
		}
		return sumOutputs;
	}
	
	public static TransactionWrapper getChannelRefundTransaction(Channel channel) throws SQLException, AddressFormatException {
		Transaction refundTransaction = new Transaction(Constants.getNetwork());
		
		refundTransaction.addOutput(Coin.valueOf(channel.getInitialAmountClient()-Tools.getTransactionFees(1, 2)), channel.getChangeAddressClientAsAddress() );
		refundTransaction.addOutput(Coin.valueOf(channel.getInitialAmountServer()), new Address(Constants.getNetwork(), channel.getChangeAddressServer() ));

		refundTransaction.setLockTime(channel.getTimestampClose());
		
		
		refundTransaction.addInput(channel.getOpeningTx().getOutput(0));
		refundTransaction.getInput(0).setSequenceNumber(0);
		
		Sha256Hash sighash = refundTransaction.hashForSignature(0, channel.getOpeningTx().getOutput(0).getScriptPubKey(), SigHash.ALL, false);
		ECKey.ECDSASignature signature = channel.getServerKeyOnServer().sign(sighash);
		
		return new TransactionWrapper(refundTransaction, signature);
	}
	//TODO: These 2 methods are pretty slow, change them to Base64 in production:
	//	http://java-performance.info/base64-encoding-and-decoding-performance/
	public static String byteToString(byte[] array) {
		return new String(Base64.encode(array));
	}
	
	public static String byteToString58(byte[] array) {
		return Base58.encode(array);
	}

	public static byte[] stringToByte(String string) {
		return Base64.decode(string);
	}
	
	public static byte[] stringToByte58(String string) {
		return Base58.decode(string);
	}
	
	public static ECDSASignature getSignature(Transaction transactionToSign, int index, TransactionOutput outputToSpend, ECKey key) {
		Sha256Hash hash = transactionToSign.hashForSignature(index, outputToSpend.getScriptPubKey(), SigHash.ALL, false);
		ECDSASignature signature = key.sign(hash);
		return new TransactionSignature(signature, SigHash.ALL, false);
	}
	
	public static boolean checkSignature(Transaction transaction, int index, TransactionOutput outputToSpend, ECKey key, byte[] signature) {
		Sha256Hash hash = transaction.hashForSignature(index, outputToSpend.getScriptBytes(), SigHash.ALL, false);
		return key.verify(hash, ECDSASignature.decodeFromDER(signature));
	}
	
	public static Script getMultisigInputScript(ECDSASignature client, ECDSASignature server) {
		ArrayList<TransactionSignature> signList = new ArrayList<TransactionSignature>();
		signList.add(new TransactionSignature(client, SigHash.ALL, false));
		signList.add(new TransactionSignature(server, SigHash.ALL, false));
		Script inputScript = ScriptBuilder.createMultiSigInputScript(signList);
		/**
		 * Seems there is a bug here,
		 * https://groups.google.com/forum/#!topic/bitcoinj/A9R8TdUsXms
		 */
		Script workaround = new Script(inputScript.getProgram());
		return workaround;
		
	}
	
	public static long getTransactionFees(int size) {
//		return (Math.ceil( ( (float) size )/1000) ) * 1000 * 500 ;
		return (long) (size * Constants.FEE_PER_BYTE) ;
	}
	
	public static boolean checkTransactionFees(int size, Transaction transaction, TransactionOutput output) {
		long in = output.getValue().value;
		long out = 0;
		for(TransactionOutput o : transaction.getOutputs()) {
			out+=o.getValue().value;
		}
		long diff = in-out;
		float f = ( (float) diff)  / size;
		
		if(f>=Constants.FEE_PER_BYTE_MIN) {
			if(f<=Constants.FEE_PER_BYTE_MAX) {
				return true;
			} 
		}
		System.out.println("Fee not correct. Total Fee: "+diff+" Per Byte: "+f+" Size: "+size);
		return false;
	}
	
	
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
	public static boolean checkTransactionLockTime(Transaction transaction, int locktime) {
		if(Math.abs(transaction.getLockTime() - locktime) > 5 * 60 ) {
			System.out.println("Locktime not correct. Should be: "+locktime+" Is: "+transaction.getLockTime()+" Diff: "+Math.abs(transaction.getLockTime() - locktime));
			return false;
		}
		
		for(TransactionInput input : transaction.getInputs()) {
			if(input.getSequenceNumber() == 0) {
				return true;
			}
		}
		System.out.println("No Sequence Number is 0..");
		return false;
	}
	
	public static Transaction setTransactionLockTime(Transaction transaction, int locktime) {
		transaction.setLockTime(locktime);
		for(TransactionInput input : transaction.getInputs()) {
			input.setSequenceNumber(0);
		}
		return transaction;
	}
	
	
	/**
	 * With reference to 
	 * http://bitcoin.stackexchange.com/questions/1195/how-to-calculate-transaction-size-before-sending
	 * 
	 * @param inputs
	 * @param outputs
	 * @return
	 */
	public static long getTransactionFees(int inputs, int outputs) {
		/**
		 * One output will pay to multi sig, 144 bytes
		 */
		int size = inputs * 180 + ( outputs - 1 ) * 34 + 144 + 10 + 40;
		return Tools.getTransactionFees(size);
	}
	
	public static boolean compareHash(Sha256Hash hash1, Sha256Hash hash2) {
		return hash1.toString().equals(hash2.toString());
	}
	
	public static String hashSecret(byte[] secret) throws UnsupportedEncodingException, NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(secret);
		byte[] digest = md.digest();
		
		RIPEMD160Digest dig = new RIPEMD160Digest();
		dig.update(digest, 0, digest.length);
		
		byte[] out = new byte[20];
		dig.doFinal(out,0);
		
		return Tools.byteToString(out);
	}
	
	public static Script getDummyScript() {
		ScriptBuilder builder = new ScriptBuilder();
		builder.smallNum(0);
		return builder.build();
	}
	

}
