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
package network.thunder.core.etc;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.TestNet3Params;

// TODO: Auto-generated Javadoc

/**
 * The Class Constants.
 */
public class Constants {

    public static final int STANDARD_PORT = 2204;

    public static final int ESCAPE_REVOCATION_TIME = 24 * 60 * 60 / 10 * 60; //In blocks..

    /**
     * The Constant SERVER_URL.
     */
    public static final String SERVER_URL = "localhost";
    /**
     * The max channel value.
     */
    public static long MAX_CHANNEL_VALUE = 1000000000;
    /**
     * The min channel value.
     */
    public static long MIN_CHANNEL_VALUE = 10000;
    /**
     * The max server share.
     */
    public static long MAX_SERVER_SHARE = 500000000;
    /**
     * The min payment.
     */
    public static long MIN_PAYMENT = 1000;
    /**
     * The min channel duration in days.
     */
    public static int MIN_CHANNEL_DURATION_IN_DAYS = 1;
    /**
     * The max channel duration in days.
     */
    public static int MAX_CHANNEL_DURATION_IN_DAYS = 100;
    /**
     * The size revoke tx.
     */
    public static int SIZE_REVOKE_TX = 150;
    /**
     * The size of settlement tx.
     */
    public static int SIZE_OF_SETTLEMENT_TX = 500;
    /**
     * The max channel creation time.
     */
    public static int MAX_CHANNEL_CREATION_TIME = 10;
    /**
     * The max channel keep time after closed.
     */
    public static int MAX_CHANNEL_KEEP_TIME_AFTER_CLOSED = 24 * 60 * 60 * 20;
    /**
     * The min confirmation time.
     */
    public static int MIN_CONFIRMATION_TIME = 0;
    /**
     * The min confirmation time for channel.
     */
    public static int MIN_CONFIRMATION_TIME_FOR_CHANNEL = 0;
    /**
     * The min keys on channel creation.
     */
    public static int MIN_KEYS_ON_CHANNEL_CREATION = 0;
    /**
     * The serverside.
     */
    public static boolean SERVERSIDE = true;
    /**
     * The clientside.
     */
    public static boolean CLIENTSIDE = false;
    /**
     * The fee per byte.
     */
    public static float FEE_PER_BYTE = 3;
    /**
     * The fee per byte min.
     */
    public static float FEE_PER_BYTE_MIN = 0.5f;
    /**
     * The fee per byte max.
     */
    public static float FEE_PER_BYTE_MAX = 15;
    /**
     * The keys per payment serverside.
     */
    public static int KEYS_PER_PAYMENT_SERVERSIDE = 2;
    /**
     * The keys per payment clientside.
     */
    public static int KEYS_PER_PAYMENT_CLIENTSIDE = 1;
    /**
     * The timeframe per key depth.
     */
    public static int TIMEFRAME_PER_KEY_DEPTH = 60 * 60 * 24;
    /**
     * The server fee percentage.
     */
    public static double SERVER_FEE_PERCENTAGE = 0.01;
    /**
     * The server fee flat.
     */
    public static long SERVER_FEE_FLAT = 100;
    /**
     * The server fee min.
     */
    public static long SERVER_FEE_MIN = 10;
    /**
     * The server fee max.
     */
    public static long SERVER_FEE_MAX = 1000;
    /**
     * The log levels.
     */
    public static int[] LOG_LEVELS = {1, 2, 3, 4, 5};
    /**
     * If channelEnd-currentTime>this, refuse to accept new payments.
     */
    public static int LOCK_TIME_BEFORE_CHANNEL_ENDS = 10 * 24 * 60 * 60;
    /**
     * All transactions lock-time should be set to the channel end minus this.
     */
    public static int SECURITY_TIME_WINDOW_BEFORE_CHANNEL_ENDS = 3 * 24 * 60 * 60;
    /**
     * This timeframe is the total amount of time for the receiver to disclose his secret
     * for the payment after we added it to his channel.
     * <p>
     * Use this timeframe, when creating the refund transaction for the serverSide,
     * for payments from the server to the client, such that the server can broadcast and claim the refund
     * afterwards.
     */
    public static int TIME_TO_REVEAL_SECRET = 4 * 24 * 60 * 60;
    /**
     * This timeframe is the total amount of time for the receiver add a payment to his channel.
     * Afterwards, the payment will refund to the client.
     */
    public static int TIME_TO_ADD_TO_CHANNEL = 2 * 24 * 60 * 60;

    //	/**
    //	 * Use this timeframe, when creating the refund transaction for the serverSide,
    //	 * 	for payments from the server to the client.
    //	 */
    //	public static int REFUND_TIME_FOR_PAYMENTS_FROM_SERVER_SERVER_SIDE = 7 * 24 * 60 * 60;
    /**
     * This timeframe is the total amount of time for the receiver add a payment to his channel.
     * Afterwards, the payment will refund to the client.
     */
    public static int TIME_TOTAL_PAYMENT = 7 * 24 * 60 * 60;

    public static int TIME_FOR_STEALING_CHANGE_OUTPUT = 7 * 24 * 60 * 60;
    public static int TIME_FOR_STEALING_PAYMENT_OUTPUT = 1 * 24 * 60 * 60;

    /**
     * Get the Network we are working on..
     *
     * @return the network
     */
    public static NetworkParameters getNetwork () {
        return TestNet3Params.get();
        //		return RegTestParams.get();
        //		return MainNetParams.get();
    }

    /**
     * Gets the time frame for validation.
     *
     * @return the time frame for validation
     */
    public static int getTimeFrameForValidation () {
        return 60;
    }

}
