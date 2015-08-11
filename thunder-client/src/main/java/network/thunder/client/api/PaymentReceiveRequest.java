package network.thunder.client.api;
//package bitrelay.api;
//
//import java.security.NoSuchAlgorithmException;
//
//import bitrelay.database.objects.Channel;
//import bitrelay.database.objects.Payment;
//import bitrelay.etc.Tools;
//
//public class PaymentReceiveRequest extends PaymentRequest {
//	private static final String PREFIX = "4";
//	
//	
//	String id;
//	String typeOfId;
//	
//	String secretHash;
//	Payment payment;
//	
//	long amount;
//	
//	
//	public PaymentReceiveRequest(Channel channel, Payment p) {
//		super(channel, p);
//		payment = p;
//		secretHash = p.getSecretHash();
//		
//		amount = p.getAmount();
//		
//		id = p.getPubKeyReceiver();
//	}
//	
//	public String getAddress() throws NoSuchAlgorithmException {
//		String a = PREFIX+typeOfId+id+secretHash;
//		String hash = Tools.getFourCharacterHash(a);
//		
//		return a+hash;
//	}
//	
//	
//}
