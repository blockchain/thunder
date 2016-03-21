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
package network.thunder.core.helper;

import network.thunder.core.communication.layer.high.RevocationHash;
import network.thunder.core.etc.Tools;

import java.nio.ByteBuffer;
import java.util.Arrays;

/*
 * For deterministic derivation of revocation hashes, use the following scheme
 * Chose a master seed - for example the hash of private key.
 * For obtaining a new childseed, hash the parentseed twice using SHA256.
 * For obtaining a sibling of a seed, concat the hash with the 4-byte integer of the sibling id.
 * The preimage will be the sibling seed hashed with RIPEMD160(SHA256) and the public hash will be the preimage hashed with RIPEMD160(SHA256) again.
 *
 * This ensures that one can give away the preimage without revealing all other siblings.
 * Revealing the childseed will reveal all further children and all siblings.
 */
public class HashDerivation {

    /**
     * The other party has breached the contract and submitted an old channel transaction.
     *
     * @param seed            The latest masterseed we received from the other party
     * @param target          The hash we are looking for
     * @param maxChildTries   The maximum depth we will search to before giving up..
     * @param maxSiblingTries The amount of siblings calculating for each depth
     * @return The preimage hashing to the desired hash.
     */
    public static RevocationHash bruteForceHash (byte[] seed, byte[] target, int maxChildTries, int maxSiblingTries) {

        for (int i = 0; i < maxChildTries; i++) {

            for (int j = 0; j < maxSiblingTries; j++) {
                RevocationHash test = HashDerivation.calculateRevocationHash(seed, 0, j);
                if (Arrays.equals(test.getSecretHash(), target)) {
                    return new RevocationHash(i, j, test.getSecret(), test.getSecretHash());
                }
            }

            seed = Tools.hashSecret(seed);

        }
        return null;
    }

    /**
     * Calculate a revocation hash for a new channel state.
     *
     * @param seed        A masterseed for this calculation
     * @param depth       The depth of the new RevocationHash
     * @param childNumber The n. sibling at the specified depth
     */
    public static RevocationHash calculateRevocationHash (byte[] seed, int depth, int childNumber) {

        byte[] childseed = seed;
        for (int i = 0; i < depth; i++) {
            childseed = Tools.hashSecret(childseed);
        }

        if (childNumber == 0) {
            return new RevocationHash(depth, childNumber, childseed, null);
        }

        byte[] childseedWithNumber = new byte[24];
        System.arraycopy(childseed, 0, childseedWithNumber, 0, 20);

        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(childNumber);
        buffer.flip();

        System.arraycopy(buffer.array(), 0, childseedWithNumber, 20, 4);

        byte[] secret = Tools.hashSecret(childseedWithNumber);
        byte[] secretHash = Tools.hashSecret(secret);

        return new RevocationHash(depth, childNumber, secret, secretHash);

    }

}
