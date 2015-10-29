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

import network.thunder.core.communication.objects.subobjects.RevocationHash;

/**
 * First request for opening a channel.
 */
public class EstablishChannelRequestOne {

    private String pubKey;
    private String pubKeyFE;
    private RevocationHash revocationHash;
    private String secretHashFE;
    private long totalAmount;
    private long clientAmount;
    private String changeAddress;

    public EstablishChannelRequestOne (String pubKey, String pubKeyFE, RevocationHash revocationHash, String secretHashFE, long totalAmount, long clientAmount, String changeAddress) {
        this.pubKey = pubKey;
        this.pubKeyFE = pubKeyFE;
        this.revocationHash = revocationHash;
        this.secretHashFE = secretHashFE;
        this.totalAmount = totalAmount;
        this.clientAmount = clientAmount;
        this.changeAddress = changeAddress;
    }

    public String getPubKey () {
        return pubKey;
    }

    public String getPubKeyFE () {
        return pubKeyFE;
    }

    public RevocationHash getRevocationHash () {
        return revocationHash;
    }

    public String getSecretHashFE () {
        return secretHashFE;
    }

    public long getTotalAmount () {
        return totalAmount;
    }

    public long getClientAmount () {
        return clientAmount;
    }

    public String getChangeAddress () {
        return changeAddress;
    }
}
