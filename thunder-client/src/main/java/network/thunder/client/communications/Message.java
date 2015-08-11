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
package network.thunder.client.communications;

import java.io.IOException;
import java.io.OutputStream;
import java.security.SignatureException;
import java.sql.Connection;

import network.thunder.client.database.MySQLConnection;
import network.thunder.client.etc.Constants;
import network.thunder.client.etc.KeyDerivation;
import network.thunder.client.etc.Tools;

import org.bitcoinj.core.ECKey;

import com.google.gson.Gson;

public class Message {
	
	public String signature;
	public String pubkey;
	public boolean success;
	public String data;
	public int type;
	public int timestamp;
	
	public Message() {}
	
	public Message(Object o, int type, ECKey key) {
        this.fill(o);
        this.success = true;
        this.type = type;
        this.sign(key);
	}
	
	public Message(Object o, int type, ECKey key, int timestamp) {
        this.fill(o);
        this.success = true;
        this.type = type;
        this.timestamp = timestamp;
		pubkey = Tools.byteToString(key.getPubKey());
		signature = key.signMessage(this.getSignatureMessage());
	}
	
	public Message(String response, Connection conn) throws Exception {
		System.out.println("Response: "+response);
		
		Message message = new Gson().fromJson(response, Message.class);
		this.signature = message.signature;
		this.pubkey = message.pubkey;
		this.success = message.success;
		this.data = message.data;
		this.type = message.type;
		this.timestamp = message.timestamp;
    	prepare(conn);
	}

	
	public void fill(Object o) {
		data = new Gson().toJson(o);
	}	
	
	private String getSignatureMessage() {
		return type+pubkey+success+timestamp+data;
	}
	
	public void sign(byte[] privateKey) {
		timestamp = Tools.currentTime();
		ECKey key = ECKey.fromPrivate(privateKey);
		pubkey = Tools.byteToString(key.getPubKey());
		signature = key.signMessage(this.getSignatureMessage());
	}

	
	public void sign(ECKey privateKey) {
		timestamp = Tools.currentTime();
		pubkey = Tools.byteToString(privateKey.getPubKey());
		signature = privateKey.signMessage(this.getSignatureMessage());
	}
	
	public void sign() {
		ECKey privateKey = KeyDerivation.getMasterKey(0);
		timestamp = Tools.currentTime();
		pubkey = Tools.byteToString(privateKey.getPubKey());
		signature = privateKey.signMessage(this.getSignatureMessage());
	}
	
	public boolean validate() {
		
		if(Math.abs(timestamp-Tools.currentTime()) > Constants.getTimeFrameForValidation() ) {
			System.out.println("Timestamp does not match! Received: "+timestamp+" Current: "+Tools.currentTime());
			return false;
		}

		ECKey key = ECKey.fromPublicOnly(Tools.stringToByte(pubkey));
		try {
			key.verifyMessage(this.getSignatureMessage(), signature);
			
		} catch (SignatureException e) {
			System.out.println("Signature does not match..");
			e.printStackTrace();
			return false;
		}
		return true;

	}
	
	/**
	 *  Call this method right after receiving it.
	 * 
	 * @param conn
	 * @param privateKey
	 * @throws Exception
	 */
	public void prepare(Connection conn) throws Exception {
		if(!this.validate()) throw new Exception("Validation failed..");
		if(!this.success) throw new Exception(this.data);
		MySQLConnection.saveMessage(conn, this);
	}


}
