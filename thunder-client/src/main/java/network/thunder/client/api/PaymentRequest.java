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
package network.thunder.client.api;

import java.security.NoSuchAlgorithmException;

import network.thunder.client.database.objects.Channel;
import network.thunder.client.database.objects.Payment;
import network.thunder.client.etc.Tools;

public class PaymentRequest {
	private static final String PREFIX = "4";
	private static final String DELIMITER = "-";
	
	
	String id;
	String typeOfId = "A";
	
	String domain = "@thunder.network";
	
	Payment payment;
	



	String secretHash58;
	
	public PaymentRequest(Channel channel, Payment p) {
		payment = p;
		
		
		id = p.getReceiver().substring(0, 10);
//		secretHash58 = Tools.byteToString58(Tools.stringToByte(payment.getSecretHash()));
		secretHash58 = payment.getSecretHash();

	}
	
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
	
	public String getAddress() throws NoSuchAlgorithmException {
		
		String a = PREFIX+typeOfId+id+secretHash58+domain;
		
		String b = PREFIX+DELIMITER+typeOfId+DELIMITER+id+DELIMITER+secretHash58+DELIMITER+domain;
		
		String hash = Tools.getFourCharacterHash(b);
		System.out.println(a);
		System.out.println(b);
		System.out.println(hash);
		
		return PREFIX+typeOfId+id+secretHash58+hash+domain;
	}
	
	
	public String getId() {
		return id;
	}

	public String getSecretHash58() {
		return secretHash58;
	}
	
	public Payment getPayment() {
		return payment;
	}
	
	

	public static boolean checkAddress(String address) {
		
		return true;
		
	}
	
	
}
