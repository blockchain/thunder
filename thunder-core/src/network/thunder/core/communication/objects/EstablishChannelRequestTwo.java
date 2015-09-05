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

import network.thunder.core.etc.Constants;
import network.thunder.core.etc.Tools;
import org.bitcoinj.core.Transaction;

/**
 * Second request for opening a channel.
 */
public class EstablishChannelRequestTwo {

	private String openingTxHash;
	private String escapeTx;
	private String escapeFastTx;

	public EstablishChannelRequestTwo (String openingTxHash, Transaction escapeTx, Transaction escapeFastTx) {
		this.openingTxHash = openingTxHash;
		this.escapeTx = Tools.byteToString(escapeTx.bitcoinSerialize());
		this.escapeFastTx = Tools.byteToString(escapeFastTx.bitcoinSerialize());
	}

	public String getOpeningTxHash () {
		return openingTxHash;
	}

	public Transaction getEscapeTx () {
		return new Transaction(Constants.getNetwork(), Tools.stringToByte(escapeTx));
	}

	public Transaction getEscapeFastTx () {
		return new Transaction(Constants.getNetwork(), Tools.stringToByte(escapeFastTx));
	}
}
