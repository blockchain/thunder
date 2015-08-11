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
import network.thunder.client.database.objects.Channel;
import network.thunder.client.database.objects.KeyWrapper;
import network.thunder.client.database.objects.Payment;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;


public class MockObjects {

	public static Channel getChannel() {
		Channel channel = new Channel();
		
		channel.setAmountClient(100000);
		channel.setAmountServer(100000);
		channel.setInitialAmountClient(100000);
		channel.setInitialAmountServer(100000);

		
		
		channel.setMasterChainDepth(51);
		channel.setMasterPrivateKeyClient("tprvAFbMAzv6Q3yxJVDF4hSSmSd4GbAMy2ywsPa5ocwce4FQmw2S8fKBfqscs4jwwZ9FqRAu4B3Sgdgqtr9o3YLonCPpSqQoTH5HSoZjbhbFyrM");
		channel.setMasterPrivateKeyServer("tprv8dYDjoA6EHAeYzmZpRGtPrVZFucLXZ8iLj8NahT7S7FiVBNfe2thE3hDtbB1tLHcqxmYKDQ9kiuWuT2pRgpgMp9fm7cR4HE96H37PTkC4Hg");
		
		channel.setChangeAddressClient("mmHUXTpJq95cudQuW1K2R5gr3xEyFJSmx9");
		channel.setChangeAddressServer("n1j4cXCTDmTcZ5RbZspXGLjEsSoe3gbedc");
		
		channel.setPaymentPhase(0);
		channel.setEstablishPhase(0);
		channel.setReady(true);
		
		channel.setTimestampOpen(1442309479);
		channel.setTimestampClose(1437989479);
		
		channel.setPubKeyClient("ArLPRfLTtF/9pGhYIsoDGi1LUzWmjP/nBxy+Bob401Zf");
		channel.setPubKeyServer("Al3QhTK5CzVkOwPRv9eikUv2QNbdJu/byIE0DokJhlVw");
		
		channel.setOpeningTxHash("e22b7597a9faa5e963461d00c1d487e509953bb7d75bf0cd58995d91f935beee");
		
		channel.setKeyChainChild(1);
		channel.setKeyChainDepth(50);
		
		channel.setOpeningTx(getOpeningTransaction());
		
		
		return channel;
	}
	
	public static Transaction getOpeningTransaction() {
		String serialized = "45zDbrVkNcVD9rGcHyfyLpJAcaqoBKnakE989roXV6mZrhzKeJMh3wZML7VRAYfnj2C2nxdYPJZAbLjV4g2ArpCkehS3YBs5AapkaVAZF5NZXB1pcyTHkyRcVJGQPC2kBrFSeo1mst24VFd5XaVe1j9z9oQg9EJnEZLKoKWiCh1aPemQqrFBekzeq9Bf4PX9Sqhzgnp6rKgF2ZYkpNeiqvuwxSAa4u79gjuyJQw5Ms8fwLgfYqsuLSH3aLxWEwMm1yYAmeWfj4YLuoamfMUBVibLjhrtYmyBWFnMjS9QhvamcQdzyewiBPvn5JEENknYAfFXtfwug4xwnbJoRaWUHK67uEudJJyqCby6ZcY9Ti5gu9bW9aqdrzmx6zUiVq6h9QUBTSzQhS6Qev3bCrrTKhB1oTCptB4jHrg61ma7gFuYGJWWX9UcDt3eeFBCiSLiRyJhED36q7JW32qVRishJmzR6L99EwSZGpe8mMXYqxfwrirjmcnPi4cbeLQiFiTbeca5Pgs69jeN1wGhbF97d8USiVv6SLEDtFoBQ7VqtgcQZemjMkZ9JCRuCY4hDCsXJZ9dKzYbBQXktiALjWLA748xFftxNwYHgtmqLmTdnF";
		return new Transaction(Constants.getNetwork(), Tools.stringToByte58(serialized));
	}
	
	public static KeyWrapper getKeyWrapper() {
		KeyWrapper keyList = new KeyWrapper();
		keyList.addKey(1, "A0wyIO3/kaUyV+wzCueUC3BIWNKxM3bRzDrx3w/kmljE");
		return keyList;
	}
	
	public static Payment getPaymentToServer() {
//		Payment payment = new Payment("ArLPRfLTtF/9pGhYIsoDGi1LUzWmjP/nBxy+Bob401Zf", "Al3QhTK5CzVkOwPRv9eikUv2QNbdJu/byIE0DokJhlVw", 1000, "DdAefFPXVU7/OVB9zzIA1n4VWFg=");
//		payment.paymentToServer = true;
//		
//		return payment;
		return null;
	}
	
	public static Payment getPaymentToClient() {
//		Payment payment = new Payment("Al3QhTK5CzVkOwPRv9eikUv2QNbdJu/byIE0DokJhlVw", "ArLPRfLTtF/9pGhYIsoDGi1LUzWmjP/nBxy+Bob401Zf", 1000, "DdAefFPXVU7/OVB9zzIA1n4VWFg=");
//		payment.paymentToServer = false;
//		
//		return payment;
		return null;
	}
	
	public static TransactionOutput getPaymentOutputServerSide() {
		String serialized = "2cot9bQnHzhdsXysES3yridYFdjnLP8PYfHajQ8J33fnFD8YgyDhdXUzgtHkzM9funpBy7z2v548oKjCo5q9RwY1xnAfVkffgryetjYnv59MmGFECxRUydRrpWVacYxQLTNpTYYdi4ZWyd6tjWC6b8BtEanPKmXaRzveD4gz8LmHsNvmhsoZgheEDVXiC3orswdoPqLrqpXJvoLfzHkYnCnX9Sq5ZF9Lk1VbxUED1bZmvGwmZFh6ogNj";
		return new TransactionOutput(Constants.getNetwork(), null, Tools.stringToByte58(serialized), 0);
	}	
	public static TransactionOutput getPaymentOutputClientSide() {
		String serialized = "qFzkW6SVTBsbxvwDth96LjM41CBATnHvwrE2Qtn8BR6yxACS2LQRCsCsQpRBFctdTPBus9hHhWXhWtCUeJjJG2xw5Cnch8QyCX4QmWwBVrUhZ3HnGpMbEw13v9j3okrB8YVmGKYUE6cnP7WrFWWg9Ptix5tZvMRFNAeSzVUk22EQLqhWm75EC1GStFjsLXADJnTAYX";
		return new TransactionOutput(Constants.getNetwork(), null, Tools.stringToByte58(serialized), 0);
	}
	
	
}
