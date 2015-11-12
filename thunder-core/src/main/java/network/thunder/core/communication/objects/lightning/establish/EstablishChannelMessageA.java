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
 * First request for opening a channel.
 */
public class EstablishChannelMessageA {

    private byte[] pubKey;
    private byte[] pubKeyFE;
    private byte[] secretHashFE;

    private long clientAmount;
    private long serverAmount;

    public EstablishChannelMessageA (byte[] pubKey, byte[] pubKeyFE, byte[] secretHashFE, long clientAmount, long serverAmount) {
        this.pubKey = pubKey;
        this.pubKeyFE = pubKeyFE;
        this.secretHashFE = secretHashFE;
        this.clientAmount = clientAmount;
        this.serverAmount = serverAmount;
    }

    //region Getter Setter
    public long getClientAmount () {
        return clientAmount;
    }

    public byte[] getPubKey () {
        return pubKey;
    }

    public byte[] getPubKeyFE () {
        return pubKeyFE;
    }

    public byte[] getSecretHashFE () {
        return secretHashFE;
    }

    public long getServerAmount () {
        return serverAmount;
    }
    //endregion
}
