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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * Class for revocation hash.
 * Even though it is in theory the same as a payment hash, we use a different class, to avoid confusion. (which would lead to a direct loss of funds..)
 */
public class RevocationHash {
    private int depth;
    private int child;
    private byte[] secret;
    private byte[] secretHash;

    public RevocationHash () {
    }

    public RevocationHash (int depth, int child, byte[] secret, byte[] secretHash) {
        this.depth = depth;
        this.child = child;
        this.secret = secret;
        this.secretHash = secretHash;
    }

    public RevocationHash (int depth, int child, byte[] secret) {
        this.depth = depth;
        this.child = child;
        this.secret = secret;
        this.secretHash = Tools.hashSecret(secret);
    }

    public RevocationHash (ResultSet set) throws SQLException {
        this.secretHash = set.getBytes("secretHash");
        this.secret = set.getBytes("secret");
        this.depth = set.getInt("depth");
        this.child = set.getInt("child");
    }

    /**
     * Check whether the supplied preimage does indeed hash to the correct hash.
     *
     * @return true, if successful
     */
    public boolean check () {
        /*
         * Child = 0 is - per convention - a new masterkey. We will check it later.
		 */
        if (child == 0) {
            return true;
        }

        if (secret == null) {
            return false;
        }

        return Arrays.equals(secretHash, Tools.hashSecret(secret));

    }

    public int getChild () {
        return child;
    }

    public int getDepth () {
        return depth;
    }

    /**
     * The preimage corresponding to the hash.
     * Losing it to the counterparty before the revocation may lead to loss of funds.
     */

    public byte[] getSecret () {
        return secret;
    }

    /**
     * The hash necessary for the transactions to be revocable.
     */
    public byte[] getSecretHash () {
        return secretHash;
    }

    @Override
    public int hashCode () {
        int result = depth;
        result = 31 * result + child;
        result = 31 * result + (secret != null ? Arrays.hashCode(secret) : 0);
        result = 31 * result + (secretHash != null ? Arrays.hashCode(secretHash) : 0);
        return result;
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

        if (depth != that.depth) {
            return false;
        }
        if (child != that.child) {
            return false;
        }
        if (!Arrays.equals(secret, that.secret)) {
            return false;
        }
        return Arrays.equals(secretHash, that.secretHash);

    }

    @Override
    public String toString () {
        return "RevocationHash{" +
                "" + Tools.bytesToHex(secretHash).substring(0, 6) + ".." +
                '}';
    }
}