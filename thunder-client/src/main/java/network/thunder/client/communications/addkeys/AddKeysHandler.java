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
package network.thunder.client.communications.addkeys;

import java.sql.Connection;

import network.thunder.client.communications.objects.AddKeysRequest;
import network.thunder.client.communications.objects.AddKeysResponse;
import network.thunder.client.database.MySQLConnection;
import network.thunder.client.database.objects.Channel;
import network.thunder.client.database.objects.Key;

public class AddKeysHandler {
	public Connection conn;
	public int amountClient;
	public int amountServer;
	public Channel channel;
	
	public AddKeysRequest request() throws Exception {

		
		AddKeysRequest request = new AddKeysRequest();
		long time = System.currentTimeMillis();
		long time1 = System.currentTimeMillis();

		request.keyList = MySQLConnection.createKeys(conn, channel, amountClient);
		request.amount = amountServer;
		
//		time1 = System.currentTimeMillis();
//		System.out.println("requestKeys request 1 1: "+(time1-time) );
//		time = System.currentTimeMillis();
		
		
		for(Key key : request.keyList) {
			key.privateKey = null;
		}
		
//		time1 = System.currentTimeMillis();
//		System.out.println("requestKeys request 1 2: "+(time1-time) );
//		time = System.currentTimeMillis();

		
		return request;		
		
	}
	
	
	public void evaluateResponse(AddKeysResponse m) throws Exception {	
		/**
		 * Check if the keys are provided and save them..
		 */	
		if(m.keyList == null)
			throw new Exception("keyList is null");
		if(m.keyList.size() == 0)
			throw new Exception("keyList is empty");

		
		for(Key key : m.keyList) {
			key.privateKey = null;
//			MySQLConnection.addKey(conn, key, channel.getPubKeyClient(), false);
		}
		
//		System.out.println("New Keys to add: "+m.keyList.size());
		
		MySQLConnection.addKey(conn, m.keyList, channel.getId(), false);

	}
}
