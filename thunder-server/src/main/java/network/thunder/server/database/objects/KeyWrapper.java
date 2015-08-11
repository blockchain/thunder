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

import java.util.ArrayList;

import network.thunder.server.etc.Tools;

import org.bitcoinj.core.ECKey;

// TODO: Auto-generated Javadoc
/**
 * The Class KeyWrapper.
 */
public class KeyWrapper {
	
	/**
	 * The Class IKey.
	 */
	public class IKey {
		
		/**
		 * The pub key.
		 */
		public String pubKey;
		
		/**
		 * The id.
		 */
		public int id;
		
		/**
		 * The used.
		 */
		public boolean used;
	}
	
	/**
	 * The key list.
	 */
	ArrayList<IKey> keyList = new ArrayList<IKey>();
	
	/**
	 * Gets the key.
	 *
	 * @return the key
	 */
	public String getKey() {
		for(IKey k : keyList) {
//			if(!k.used) {
				k.used = true;
				return k.pubKey;
//			}
		}
		return null;
	}
	
	/**
	 * Gets the used ec key.
	 *
	 * @param pubKey the pub key
	 * @return the used ec key
	 */
	public ECKey getUsedECKey(String pubKey) {
		for(IKey k : keyList) {
			if(k.used) {
				if(k.pubKey.equals(pubKey)) {
					ECKey key = ECKey.fromPrivate(Tools.stringToByte(pubKey));
					return key;
				}
			}
		
		}
		return null;
	}
	
	/**
	 * Adds the key.
	 *
	 * @param id the id
	 * @param pubKey the pub key
	 */
	public void addKey(int id, String pubKey) {
		IKey k = new IKey();
		k.id = id;
		k.pubKey = pubKey;
		keyList.add(k);
	}
	
	/**
	 * Check key.
	 *
	 * @param pubKey the pub key
	 * @return true, if successful
	 */
	public boolean checkKey(String pubKey) {
		for(IKey k : keyList) {
//			if(!k.used) {
				if(k.pubKey.equals(pubKey)) {
					k.used = true;
					return true;
				}
//			}
		
		}
		return false;
	}
	
	/**
	 * Gets the key list.
	 *
	 * @return the key list
	 */
	public ArrayList<IKey> getKeyList() {
		return keyList;
	}

	
	

}
