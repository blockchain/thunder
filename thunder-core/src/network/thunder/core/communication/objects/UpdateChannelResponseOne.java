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
package network.thunder.core.communication.objects;

import network.thunder.core.communication.objects.subobjects.PaymentData;
import network.thunder.core.communication.objects.subobjects.RevocationHash;
import network.thunder.core.etc.Constants;
import network.thunder.core.etc.Tools;
import org.bitcoinj.core.Transaction;

import java.util.ArrayList;

/**
 * The Class UpdateChannelResponseOne.
 */
public class UpdateChannelResponseOne {

	private RevocationHash newHash;
	private ArrayList<String> revealedSecrets;
	private ArrayList<String> removedPayments;
	private ArrayList<PaymentData> newPayments;
	private String channelTx;

	public UpdateChannelResponseOne (RevocationHash newHash, ArrayList<String> revealedSecrets, ArrayList<String> removedPayments, ArrayList<PaymentData>
			newPayments, Transaction channelTx) {
		this.newHash = newHash;
		this.revealedSecrets = revealedSecrets;
		this.removedPayments = removedPayments;
		this.newPayments = newPayments;
		this.channelTx = Tools.byteToString(channelTx.bitcoinSerialize());
	}

	public RevocationHash getNewHash () {
		return newHash;
	}

	public ArrayList<String> getRevealedSecrets () {
		return revealedSecrets;
	}

	public ArrayList<String> getRemovedPayments () {
		return removedPayments;
	}

	public ArrayList<PaymentData> getNewPayments () {
		return newPayments;
	}

	public Transaction getChannelTx () {
		return new Transaction(Constants.getNetwork(), Tools.stringToByte(channelTx));
	}
}
