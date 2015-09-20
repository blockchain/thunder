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

public class PaymentSecret {

	private String secret;
	private String hash;

	public PaymentSecret (String secret, String hash) {
		this.secret = secret;
		this.hash = hash;
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
