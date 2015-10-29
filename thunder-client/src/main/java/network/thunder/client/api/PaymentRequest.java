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

import network.thunder.client.database.objects.Channel;
import network.thunder.client.database.objects.Payment;
import network.thunder.client.etc.Tools;

import java.security.NoSuchAlgorithmException;

public class PaymentRequest {
    private static final String PREFIX = "4";
    private static final String DELIMITER = "-";

    byte[] id = new byte[8];
    byte[] secretHash = new byte[20];
    byte[] hash = new byte[4];
    byte typeOfId = 0x00;

    String domain = "@thunder.network";

    Payment payment;

    public PaymentRequest (Channel channel, Payment p) {
        payment = p;

        id = Tools.stringToByte(p.getReceiver());
        secretHash = Tools.stringToByte(p.getSecretHash());

        //		secretHash58 = Tools.byteToString58(Tools.stringToByte(payment.getSecretHash()));
        //		secretHash58 = payment.getSecretHash();

    }

    public PaymentRequest (Channel channel, long amount, String request) {

        byte[] totalWithHash = Tools.stringToByte58(request.split("@")[0]);

        System.arraycopy(totalWithHash, 1, id, 0, 8);
        System.arraycopy(totalWithHash, 9, secretHash, 0, 20);

        String secretHashB64 = Tools.byteToString(secretHash);
        String idB64 = Tools.byteToString(id);

        //		System.out.println("SecretHash: "+secretHashB64);

        payment = new Payment(channel.getId(), idB64, amount, secretHashB64);

    }

    public static boolean checkAddress (String address) {

        return true;

    }

    /**
     * Returns the address for this payment request.
     * <p>
     * Current Format of the address:
     * <p>
     * 1 byte to declare which kind of identifier we use (see (1) above)
     * 8 byte to clearly specify a receiver
     * 20 byte, the hashed preimage needed for the payment
     * 4 byte to serve as a checksum against typing errors
     * <p>
     * Base58 encoded for practical reasons.
     * Domain of the PaymentHub of the receiver at the end.
     */
    public String getAddress () throws NoSuchAlgorithmException {

        byte[] totalWithoutHash = new byte[29];
        totalWithoutHash[0] = typeOfId;
        System.arraycopy(id, 0, totalWithoutHash, 1, 8);
        System.arraycopy(secretHash, 0, totalWithoutHash, 9, 20);

        String secretHashB64 = Tools.byteToString(secretHash);

        //        System.out.println("SecretHash: " + secretHashB64);

        byte[] totalWithHash = new byte[33];

        System.arraycopy(totalWithoutHash, 0, totalWithHash, 0, 29);
        byte[] hash = Tools.getSha256Hash(totalWithoutHash);
        System.arraycopy(hash, 0, totalWithHash, 29, 4);

        String address = Tools.byteToString58(totalWithHash);

        //		String a = PREFIX+typeOfId+id+secretHash58+domain;
        //
        //		String b = PREFIX+DELIMITER+typeOfId+DELIMITER+id+DELIMITER+secretHash58+DELIMITER+domain;
        //
        //		String hash = Tools.getFourCharacterHash(b);
        //		System.out.println(a);
        //		System.out.println(b);
        //		System.out.println(hash);

        return address + domain;
    }

    public byte[] getId () {
        return id;
    }

    public Payment getPayment () {
        return payment;
    }

    public String getPaymentURI () throws NoSuchAlgorithmException {
        return "thunder:address=" + getAddress() + "&amount=" + payment.getAmount();
    }

    public byte[] getSecretHash () {
        return secretHash;
    }

}
