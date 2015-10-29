/*
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
package network.thunder.core.communication.objects;

import network.thunder.core.communication.objects.subobjects.PaymentData;
import network.thunder.core.communication.objects.subobjects.RevocationHash;

/**
 * Making a new payment.
 * First request to the other node.
 */
public class SendPaymentRequestOne {

    private RevocationHash newHash;
    private PaymentData paymentData;

    public SendPaymentRequestOne (RevocationHash newHash, PaymentData paymentData) {
        this.newHash = newHash;
        this.paymentData = paymentData;
    }

    public RevocationHash getNewHash () {
        return newHash;
    }

    public PaymentData getPaymentData () {
        return paymentData;
    }
}
