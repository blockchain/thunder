/*
 *  ThunderNetwork - Server Client Architecture to send Off-Chain Bitcoin Payments
 *  Copyright (C) 2015 Mats Jerratsch <matsjj@gmail.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *  
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package network.thunder.server.database.objects;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import network.thunder.server.etc.Tools;

// TODO: Auto-generated Javadoc
/**
 * The Class Secret.
 */
public class Secret {
	
	/**
	 * The secret.
	 */
	public String secret;
	
	/**
	 * The secret hash.
	 */
	public String secretHash;
	
	
	/**
	 * Verify.
	 *
	 * @return true, if successful
	 * @throws UnsupportedEncodingException the unsupported encoding exception
	 * @throws NoSuchAlgorithmException the no such algorithm exception
	 */
	public boolean verify() throws UnsupportedEncodingException, NoSuchAlgorithmException {
		return secretHash.equals(Tools.hashSecret(Tools.stringToByte(secret)));
	}
	
	/**
	 * Instantiates a new secret.
	 */
	public Secret() {}
	
	/**
	 * Instantiates a new secret.
	 *
	 * @param secretHash the secret hash
	 * @param secret the secret
	 */
	public Secret(String secretHash, String secret) {
		this.secret = secret;
		this.secretHash = secretHash;
	}
}
