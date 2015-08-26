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
package network.thunder.client.etc;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.TestNet3Params;

public class Constants {
	/**
	 * Get the Network we are working on..
	 * 
	 * @return
	 */
	public static NetworkParameters getNetwork() {
		return TestNet3Params.get();
//		return RegTestParams.get();
//		return MainNetParams.get();
	}
	
	public static int getTimeFrameForValidation() {
		return 60;
	}


//    public static final String SERVER_URL = "thunder.network";
    public static final String SERVER_URL = "localhost";

	
	public static long MAX_CHANNEL_VALUE = 1000000000;
	public static long MIN_CHANNEL_VALUE = 10000;
	public static long MAX_SERVER_SHARE = 500000000;
	public static long MIN_PAYMENT = 1000;
	
	public static int MIN_CHANNEL_DURATION_IN_DAYS = 1;
	public static int MAX_CHANNEL_DURATION_IN_DAYS = 100;
	
	public static int SIZE_REVOKE_TX = 150;
	public static int SIZE_OF_SETTLEMENT_TX = 500;
	
	
	public static int MAX_CHANNEL_CREATION_TIME = 10;
	public static int MAX_CHANNEL_KEEP_TIME_AFTER_CLOSED = 24 * 60 * 60 * 20;
	public static int MIN_CONFIRMATION_TIME = 0;
	public static int MIN_CONFIRMATION_TIME_FOR_CHANNEL = 0;
	public static int MIN_KEYS_ON_CHANNEL_CREATION = 0;
	
	public static boolean SERVERSIDE = true;
	public static boolean CLIENTSIDE = false;
	
	public static float FEE_PER_BYTE = 3;
	public static float FEE_PER_BYTE_MIN = 0.5f;
	public static float FEE_PER_BYTE_MAX = 15;
	
	public static int KEYS_PER_PAYMENT_SERVERSIDE = 2;
	public static int KEYS_PER_PAYMENT_CLIENTSIDE = 1;
	
	public static int TIMEFRAME_PER_KEY_DEPTH = 60 * 60 * 24 ;

	public static double SERVER_FEE_PERCENTAGE = 0.01;
	public static long SERVER_FEE_FLAT = 100;
	public static long SERVER_FEE_MIN = 10;
	public static long SERVER_FEE_MAX = 1000;
	public static int[] LOG_LEVELS =  { 2,3,4,5 };
	
	/**
	 * If channelEnd-currentTime>this, refuse to accept new payments
	 */
	public static int LOCK_TIME_BEFORE_CHANNEL_ENDS = 10 * 24 * 60 * 60;
	
	/**
	 * All transactions lock-time should be set to the channel end minus this.
	 */
	public static int SECURITY_TIME_WINDOW_BEFORE_CHANNEL_ENDS = 3 * 24 * 60 * 60;
	
//	/**
//	 * Use this timeframe, when creating the refund transaction for the serverSide, 
//	 * 	for payments from the server to the client.
//	 */
//	public static int REFUND_TIME_FOR_PAYMENTS_FROM_SERVER_SERVER_SIDE = 7 * 24 * 60 * 60;
	
	/**
	 * This timeframe is the total amount of time for the receiver to disclose his secret
	 * 	for the payment after we added it to his channel.
	 * 
	 * Use this timeframe, when creating the refund transaction for the serverSide, 
	 * 	for payments from the server to the client, such that the server can broadcast and claim the refund
	 * 	afterwards.
	 */
	public static int TIME_TO_REVEAL_SECRET = 4 * 24 * 60 * 60;	
	
	/**
	 * This timeframe is the total amount of time for the receiver add a payment to his channel.
	 * Afterwards, the payment will refund to the client.
	 */
	public static int TIME_TO_ADD_TO_CHANNEL = 2 * 24 * 60 * 60;
	
	/**
	 * This timeframe is the total amount of time for the receiver add a payment to his channel.
	 * Afterwards, the payment will refund to the client.
	 */
	public static int TIME_TOTAL_PAYMENT = 7 * 24 * 60 * 60;
	
	

}
