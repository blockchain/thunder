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
package network.thunder.client.communications.sendpayment;

import java.sql.Connection;

import network.thunder.client.communications.objects.SendPaymentRequestThree;
import network.thunder.client.communications.objects.SendPaymentResponseThree;
import network.thunder.client.database.MySQLConnection;
import network.thunder.client.database.objects.Channel;
/**			
 * Third Request for making a payment.
 * 
 * Request: 	Keys of old channels, that need to be sent in order to revoke these channels
 * 
 * Response: 	Keys of old channels, that need to be sent in order to revoke these channels
 * 							 
 * @author PC
 *
 */
public class SendPaymentHandlerThree {
	
	public Connection conn;
	public Channel channel;
	
	public SendPaymentRequestThree request() throws Exception {

		SendPaymentRequestThree m = new SendPaymentRequestThree();
		
		
		m.keyList = MySQLConnection.getKeysOfUsToBeExposed(conn, channel, false);

		return m;
	}
	
	public void evaluateResponse(SendPaymentResponseThree m) throws Exception {
		
//		if(m.keyList == null)
//			throw new Exception("keyList is null");
//		if(m.keyList.size() == 0)
//			throw new Exception("keyList is empty");

		
		MySQLConnection.checkKeysFromOtherSide(conn, channel, m.keyList);

		
		MySQLConnection.updateChannel(conn, channel);

		
		

	}
	
	

}
