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

// TODO: Auto-generated Javadoc

import network.thunder.core.communication.objects.subobjects.RevocationHash;

/**
 * Response to first request for opening a channel.
 */
public class EstablishChannelResponseOne {

    private String pubKey;
    private String pubKeyFE;
    private RevocationHash revocationHash;
    private String secretHashFE;
    private String changeAddress;
    private String openingTxHash;

    public EstablishChannelResponseOne (String pubKey, String pubKeyFE, RevocationHash revocationHash, String secretHashFE, String changeAddress, String openingTxHash) {
        this.pubKey = pubKey;
        this.pubKeyFE = pubKeyFE;
        this.revocationHash = revocationHash;
        this.secretHashFE = secretHashFE;
        this.changeAddress = changeAddress;
        this.openingTxHash = openingTxHash;
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

    public String getChangeAddress () {
        return changeAddress;
    }

    public String getOpeningTxHash () {
        return openingTxHash;
    }
}
