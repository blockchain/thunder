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

package network.thunder.core.communication.objects.subobjects;

public class PaymentData {

    private String receiver;
    private long amount;
    private long fee;
    private PaymentSecret secret;

    /**
     * TODO: We probably need further fields here, can't think of any now..
     */

    public PaymentData (String receiver, long amount, long fee, PaymentSecret secret) {
        this.receiver = receiver;
        this.amount = amount;
        this.fee = fee;
        this.secret = secret;
    }

    public String getReceiver () {
        return receiver;
    }

    public long getAmount () {
        return amount;
    }

    public long getFee () {
        return fee;
    }

    public PaymentSecret getSecret () {
        return secret;
    }
}
