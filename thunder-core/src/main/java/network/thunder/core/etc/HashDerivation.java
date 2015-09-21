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
package network.thunder.core.etc;

import network.thunder.core.communication.objects.subobjects.RevocationHash;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicHierarchy;
import org.bitcoinj.crypto.DeterministicKey;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

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

		byte[] childseedWithNumber = new byte[24];
		System.arraycopy(childseed, 0, childseedWithNumber, 0, 20);

		//Copied over from http://stackoverflow.com/questions/2183240/java-integer-to-byte-array
		//Workaround, as there is no native way to get integer to a byte array...
		byte[] conv = ByteBuffer.allocate(4).putInt(childNumber).array();

		System.arraycopy(conv, 0, childseedWithNumber, 20, 4);

		byte[] secret = Tools.hashSecret(childseedWithNumber);
		byte[] secretHash = Tools.hashSecret(secret);

		return new RevocationHash(depth, childNumber, secret, secretHash);

	}

	/**
	 * The other party has breached the contract and submitted an old channel transaction.
	 *
	 * @param seed            The latest masterseed we received from the other party
	 * @param target          The hash we are looking for
	 * @param maxChildTries   The maximum depth we will search to before giving up..
	 * @param maxSiblingTries The amount of siblings calculating for each depth
	 * @return The preimage hashing to the desired hash.
	 */
	public static byte[] bruteForceHash (byte[] seed, byte[] target, int maxChildTries, int maxSiblingTries) {

		for (int i = 0; i < maxChildTries; i++) {

			for (int j = 0; j < maxSiblingTries; j++) {

			}

			seed = Tools.hashSecret(seed);

		}
	}

	/**
	 * Brute force key.
	 *
	 * @param masterKey the master key
	 * @param pubkey    the pubkey
	 * @return the EC key
	 */
	public static ECKey bruteForceKey (String masterKey, String pubkey) {
		DeterministicKey hd = DeterministicKey.deserializeB58(masterKey, Constants.getNetwork());

		DeterministicHierarchy hi = new DeterministicHierarchy(hd);

		for (int j = 0; j < 1000; ++j) {
			List<ChildNumber> childList = getChildList(j);

			for (int i = 0; i < 1000; ++i) {

				ChildNumber childNumber = new ChildNumber(i, true);
				childList.set(j, childNumber);

				DeterministicKey key = hi.get(childList, true, true);
				String pubTemp = Tools.byteToString(key.getPubKey());
				if (pubTemp.equals(pubkey)) {
					return key;
				}
			}
		}

		return null;
	}

	/**
	 * Call to get the MasterKey for a new Channel.
	 *
	 * @param number Query the Database to get the latest unused number
	 * @return DeterministicKey for the new Channel
	 */
	public static DeterministicKey getMasterKey (int number) {

		DeterministicKey hd = DeterministicKey.deserializeB58(SideConstants.KEY_B58, Constants.getNetwork());
		//		DeterministicKey hd =  DeterministicKey.deserializeB58(null,KEY_B58);
		//        DeterministicKey hd = HDKeyDerivation.createMasterPrivateKey(KEY.getBytes());
		DeterministicHierarchy hi = new DeterministicHierarchy(hd);

		List<ChildNumber> childList = new ArrayList<ChildNumber>();
		ChildNumber childNumber = new ChildNumber(number, true);
		childList.add(childNumber);

		DeterministicKey key = hi.get(childList, true, true);
		return key;

	}

}