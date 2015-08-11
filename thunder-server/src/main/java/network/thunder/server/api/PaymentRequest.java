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
package network.thunder.server.api;

import java.security.NoSuchAlgorithmException;

import network.thunder.server.database.objects.Channel;
import network.thunder.server.database.objects.Payment;
import network.thunder.server.etc.Tools;

// TODO: Auto-generated Javadoc
/**
 * The Class PaymentRequest.
 */
public class PaymentRequest {
	
	/**
	 * The Constant PREFIX.
	 */
	private static final String PREFIX = "4";
	
	/**
	 * The Constant DELIMITER.
	 */
	private static final String DELIMITER = "-";
	
	
	/**
	 * The id.
	 */
	String id;
	
	/**
	 * The type of id.
	 */
	String typeOfId = "A";
	
	/**
	 * The domain.
	 */
	String domain = "@thunder.network";
	
	/**
	 * The payment.
	 */
	Payment payment;
	



	/**
	 * The secret hash58.
	 */
	String secretHash58;
	
	/**
	 * Instantiates a new payment request.
	 *
	 * @param channel the channel
	 * @param p the p
	 */
	public PaymentRequest(Channel channel, Payment p) {
		payment = p;
		
		
		id = p.getReceiver().substring(0, 10);
//		secretHash58 = Tools.byteToString58(Tools.stringToByte(payment.getSecretHash()));
		secretHash58 = payment.getSecretHash();

	}
	
	/**
	 * Instantiates a new payment request.
	 *
	 * @param channel the channel
	 * @param amount the amount
	 * @param request the request
	 */
	public PaymentRequest(Channel channel, long amount, String request) {
		
		id = request.substring(2, 12);
		secretHash58 = request.substring(12, 40);
		System.out.println(id);
		System.out.println(secretHash58);
//		String secretHash = Tools.byteToString(Tools.stringToByte58(secretHash58));
		String secretHash = secretHash58;
		
		System.out.println(secretHash);
		System.out.println(Tools.stringToByte(secretHash).length);
		
		payment = new Payment(channel.getId(), id, amount, secretHash);
		
		
	}
	
	/**
	 * Gets the address.
	 *
	 * @return the address
	 * @throws NoSuchAlgorithmException the no such algorithm exception
	 */
	public String getAddress() throws NoSuchAlgorithmException {
		
		String a = PREFIX+typeOfId+id+secretHash58+domain;
		
		String b = PREFIX+DELIMITER+typeOfId+DELIMITER+id+DELIMITER+secretHash58+DELIMITER+domain;
		
		String hash = Tools.getFourCharacterHash(b);
		System.out.println(a);
		System.out.println(b);
		System.out.println(hash);
		
		return PREFIX+typeOfId+id+secretHash58+hash+domain;
	}
	
	
	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * Gets the secret hash58.
	 *
	 * @return the secret hash58
	 */
	public String getSecretHash58() {
		return secretHash58;
	}
	
	/**
	 * Gets the payment.
	 *
	 * @return the payment
	 */
	public Payment getPayment() {
		return payment;
	}
	
	

	/**
	 * Check address.
	 *
	 * @param address the address
	 * @return true, if successful
	 */
	public static boolean checkAddress(String address) {
		
		return true;
		
	}
	
	
}
