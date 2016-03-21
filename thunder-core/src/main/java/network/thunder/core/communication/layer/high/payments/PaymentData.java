package network.thunder.core.communication.layer.high.payments;/*
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

import network.thunder.core.communication.layer.high.payments.messages.OnionObject;

public class PaymentData implements Cloneable {

    public boolean sending;
    public long amount;
    public long fee;

    public PaymentSecret secret;
    public int timestampOpen;
    public int timestampRefund; //timestamp at which the other party will consider this payment refunded
    public int csvDelay; //revocation delay for dual-tx

    public OnionObject onionObject;

	/*
     * TODO: We probably need further fields here, can't think of any now..
	 */

    @Override
    public boolean equals (Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PaymentData that = (PaymentData) o;

        return secret != null ? secret.equals(that.secret) : that.secret == null;

    }

    @Override
    public int hashCode () {
        return secret != null ? secret.hashCode() : 0;
    }

    @Override
    public String toString () {
        return "PaymentData{" +
                "sending=" + sending +
                ", amount=" + amount +
                '}';
    }

    @Override
    public Object clone () throws CloneNotSupportedException {
        PaymentData p = new PaymentData();
        p.onionObject = onionObject;
        p.sending = sending;
        p.amount = amount;
        p.csvDelay = csvDelay;
        p.timestampOpen = timestampOpen;
        p.timestampRefund = timestampRefund;
        p.secret = secret;
        p.fee = fee;
        return p;
    }

    public PaymentData cloneObject () {
        try {
            PaymentData paymentData = (PaymentData) this.clone();
            return paymentData;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
