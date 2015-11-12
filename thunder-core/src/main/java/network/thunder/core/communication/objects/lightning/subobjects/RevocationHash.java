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

	public int getDepth () {
		return depth;
	}

	public int getChild () {
		return child;
	}

	public String getSecret () {
		return secret;
	}

	public String getSecretHash () {
		return secretHash;
	}
}
