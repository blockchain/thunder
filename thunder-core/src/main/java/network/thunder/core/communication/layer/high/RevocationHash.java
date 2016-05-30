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

package network.thunder.core.communication.layer.high;

import network.thunder.core.etc.Tools;

import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import static network.thunder.core.etc.Tools.hashSecret;

/**
 * Class for revocation hash.
 * Even though it is in theory the same as a payment hash, we use a different class, to avoid confusion. (which would lead to a direct loss of funds..)
 */
public class RevocationHash {
    public int index;
    public byte[] secret;
    public byte[] secretHash;

    public RevocationHash (int depth, byte[] secret, byte[] secretHash) {
        this.index = depth;
        this.secret = secret;
        this.secretHash = secretHash;
    }

    public RevocationHash (int index, byte[] seed) {
        this.index = index;

        //TODO implement shachain here
        ByteBuffer byteBuffer = ByteBuffer.allocate(4 + seed.length);
        byteBuffer.putInt(index);
        byteBuffer.put(seed);
        this.secret = hashSecret(byteBuffer.array());
        this.secretHash = hashSecret(this.secret);

    }

    public RevocationHash (ResultSet set) throws SQLException {
        this.secretHash = set.getBytes("secretHash");
        this.secret = set.getBytes("secret");
        this.index = set.getInt("index");
    }

    public RevocationHash copy () {
        return new RevocationHash(this.index, this.secret, this.secretHash);
    }

    /**
     * Check whether the supplied preimage does indeed hash to the correct hash.
     *
     * @return true, if successful
     */
    public boolean check () {
        return secret != null && Arrays.equals(secretHash, hashSecret(secret));
    }

    public RevocationHash (byte[] secretHash) {
        this.secretHash = secretHash;
    }

    @Override
    public String toString () {
        return "R{" + index + ": " +
                "<" + Tools.bytesToHex(secretHash).substring(0, 5)+">" +
                '}';
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RevocationHash that = (RevocationHash) o;

        return Arrays.equals(secretHash, that.secretHash);

    }

    @Override
    public int hashCode () {
        return Arrays.hashCode(secretHash);
    }
}