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

import network.thunder.client.communications.objects.SendPaymentRequestFour;
import network.thunder.client.communications.objects.SendPaymentResponseFour;

/**
 * Fourth Request for making a payment.
 * This just means that we are okay with the data we got sent.
 * Making this request marks the payment as final on both sides.
 *
 * @author PC
 */
public class SendPaymentHandlerFour {

	public void evaluateResponse (SendPaymentResponseFour m) throws Exception {

	}

	public SendPaymentRequestFour request () throws Exception {

		SendPaymentRequestFour m = new SendPaymentRequestFour();
		return m;
	}

}
