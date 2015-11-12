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
package network.thunder.core.communication.objects.lightning.establish;

/**
 * Response to the second request for establishing a channel.
 */
public class EstablishChannelMessageD {

    private byte[] anchorHash;
    private byte[] signatureE;
    private byte[] signatureFE;

    public EstablishChannelMessageD (byte[] anchorHash, byte[] signatureE, byte[] signatureFE) {
        this.anchorHash = anchorHash;
        this.signatureE = signatureE;
        this.signatureFE = signatureFE;
    }

    //region Getter
    public byte[] getAnchorHash () {
        return anchorHash;
    }

    public byte[] getSignatureE () {
        return signatureE;
    }

    public byte[] getSignatureFE () {
        return signatureFE;
    }
    //endregion
}
