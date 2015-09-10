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

/**
 * Class for revocation hash.
 * Even though it is in theory the same as a payment hash, we use a different class, to avoid confusion. (which would lead to a direct loss of funds..)
 *
 */
public class RevocationHash {
	private int depth;
	private int child;
	private String secret;
	private String secretHash;

	public RevocationHash (int depth, int child, String secret, String secretHash) {
		this.depth = depth;
		this.child = child;
		this.secret = secret;
		this.secretHash = secretHash;
	}


	public RevocationHash (int depth, int child, byte[] secret, byte[] secretHash) {
		this.depth = depth;
		this.child = child;
		this.secret = Tools.byteToString(secret);
		this.secretHash = Tools.byteToString(secretHash);
	}

	public RevocationHash (ResultSet set) throws SQLException {
		this.secretHash = Tools.byteToString(set.getBytes("pub_key"));
		this.secret = Tools.byteToString(set.getBytes("priv_key"));
		this.depth = set.getInt("depth");
		this.child = set.getInt("child");
	}

	public int getDepth () {
		return depth;
	}

	public int getChild () {
		return child;
	}


	public String getSecretAsString () {
		return secret;
	}


	public String getSecretHashAsString () {
		return secretHash;
	}
	/**
	 * The preimage corresponding to the hash.
	 * Losing it to the counterparty before the revocation may lead to loss of funds.
	 */

	public byte[] getSecret() {
		return Tools.stringToByte(secret);
	}
	/**
	 * The hash necessary for the transactions to be revocable.
	 */
	public byte[] getSecretHash() {
		return Tools.stringToByte(secretHash);
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

		if (privateKey == null) {
			return false;
		}

		if (Arrays.equals(publicKey, Tools.hashSecret(privateKey))) {
			return true;
		}

		return false;

	}
}
