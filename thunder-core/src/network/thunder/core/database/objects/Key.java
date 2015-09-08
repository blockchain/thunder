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
package network.thunder.core.database.objects;

import network.thunder.core.etc.Tools;
import org.bitcoinj.core.ECKey;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * The Class Key.
 */
public class Key {

	/**
	 * The public key.
	 */
	public String publicKey;

	/**
	 * The private key.
	 */
	public String privateKey;

	/**
	 * The depth.
	 */
	public int depth;

	/**
	 * The child.
	 */
	public int child;

	/**
	 * Instantiates a new key.
	 */
	public Key () {
	}

	public Key (ResultSet set) throws SQLException {
		this.publicKey = set.getString("pub_key");
		this.privateKey = set.getString("priv_key");
		this.depth = set.getInt("depth");
		this.child = set.getInt("child");
	}

	/**
	 * Check.
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

		ECKey key = ECKey.fromPrivate(Tools.stringToByte(privateKey));

		if (publicKey.equals(Tools.hashSecret(Tools.stringToByte(privateKey)))) {
			return true;
		}

		return false;

	}

}
