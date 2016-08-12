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
import network.thunder.core.communication.layer.high.payments.updates.PaymentNew;
import network.thunder.core.database.objects.PaymentStatus;

public class PaymentData implements Cloneable {
    public int paymentId;

    public boolean sending;
    public long amount;

    public PaymentSecret secret;
    public int timestampOpen;
    public int timestampRefund; //timestamp at which the other party will consider this payment refunded
    public int timestampSettled;

    public PaymentStatus status = PaymentStatus.UNKNOWN;

    public OnionObject onionObject;

    public PaymentData () {
    }

    public PaymentData (PaymentNew paymentNew, boolean ourPayment) {
        this.amount = paymentNew.amount;
        this.secret = paymentNew.secret;
        this.onionObject = paymentNew.onionObject;
        this.timestampRefund = paymentNew.timestampRefund;

        this.sending = ourPayment;
    }

    public PaymentNew getPaymentNew () {
        PaymentNew paymentNew = new PaymentNew();
        paymentNew.timestampRefund = this.timestampRefund;
        paymentNew.onionObject = this.onionObject;
        paymentNew.amount = this.amount;
        paymentNew.secret = this.secret;
        return paymentNew;
    }

    @Override
    public String toString () {
        return "PaymentData{" +
                "sending=" + sending +
                ", amount=" + amount +
                ", secret=" + secret +
                ", refund=" + timestampRefund+
                ", status="+status+
                '}';
    }

    @Override
    public Object clone () throws CloneNotSupportedException {
        PaymentData p = new PaymentData();
        p.paymentId = paymentId;
        p.status = status;
        p.onionObject = onionObject;
        p.sending = sending;
        p.amount = amount;
        p.timestampOpen = timestampOpen;
        p.timestampRefund = timestampRefund;
        p.secret = secret.copy();
        return p;
    }

    public PaymentData reverse () {
        PaymentData p = this.cloneObject();
        p.sending = !p.sending;
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

    @Override
    public boolean equals (Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PaymentData that = (PaymentData) o;

        if (paymentId != that.paymentId) {
            return false;
        }
        if (sending != that.sending) {
            return false;
        }
        if (amount != that.amount) {
            return false;
        }
        if (timestampOpen != that.timestampOpen) {
            return false;
        }
        if (timestampRefund != that.timestampRefund) {
            return false;
        }
        if (timestampSettled != that.timestampSettled) {
            return false;
        }
        if (secret != null ? !secret.equals(that.secret) : that.secret != null) {
            return false;
        }
        if (status != that.status) {
            return false;
        }
        return onionObject != null ? onionObject.equals(that.onionObject) : that.onionObject == null;

    }

    @Override
    public int hashCode () {
        int result = paymentId;
        result = 31 * result + (sending ? 1 : 0);
        result = 31 * result + (int) (amount ^ (amount >>> 32));
        result = 31 * result + (secret != null ? secret.hashCode() : 0);
        result = 31 * result + timestampOpen;
        result = 31 * result + timestampRefund;
        result = 31 * result + timestampSettled;
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (onionObject != null ? onionObject.hashCode() : 0);
        return result;
    }
}
