package network.thunder.core.communication.objects.lightning.subobjects;/*
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

import network.thunder.core.communication.objects.messages.impl.message.lnpayment.OnionObject;
import network.thunder.core.communication.objects.subobjects.PaymentSecret;

public class PaymentData {

    public boolean sending;
    public long amount;
    public long fee;

    public PaymentSecret secret;
    public int timestampOpen;
    public int timestampRefund;
    public int csvDelay;

    public OnionObject onionObject;

	/*
     * TODO: We probably need further fields here, can't think of any now..
	 */

}
