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
package network.thunder.server.communications;

// TODO: Auto-generated Javadoc

/**
 * The Class Type.
 */
public class Type {

	/**
	 * The failure.
	 */
	public static int FAILURE = 000;

	/**
	 * The establish channel one request.
	 */
	public static int ESTABLISH_CHANNEL_ONE_REQUEST = 110;

	/**
	 * The establish channel one response.
	 */
	public static int ESTABLISH_CHANNEL_ONE_RESPONSE = 111;

	/**
	 * The establish channel two request.
	 */
	public static int ESTABLISH_CHANNEL_TWO_REQUEST = 120;

	/**
	 * The establish channel two response.
	 */
	public static int ESTABLISH_CHANNEL_TWO_RESPONSE = 121;

	/**
	 * The establish channel three request.
	 */
	public static int ESTABLISH_CHANNEL_THREE_REQUEST = 130;

	/**
	 * The establish channel three response.
	 */
	public static int ESTABLISH_CHANNEL_THREE_RESPONSE = 131;

	/**
	 * The send payment one request.
	 */
	public static int SEND_PAYMENT_ONE_REQUEST = 210;

	/**
	 * The send payment one response.
	 */
	public static int SEND_PAYMENT_ONE_RESPONSE = 211;

	/**
	 * The send payment two request.
	 */
	public static int SEND_PAYMENT_TWO_REQUEST = 220;

	/**
	 * The send payment two response.
	 */
	public static int SEND_PAYMENT_TWO_RESPONSE = 221;

	/**
	 * The send payment three request.
	 */
	public static int SEND_PAYMENT_THREE_REQUEST = 230;

	/**
	 * The send payment three response.
	 */
	public static int SEND_PAYMENT_THREE_RESPONSE = 231;

	/**
	 * The send payment four request.
	 */
	public static int SEND_PAYMENT_FOUR_REQUEST = 240;

	/**
	 * The send payment four response.
	 */
	public static int SEND_PAYMENT_FOUR_RESPONSE = 241;

	/**
	 * The add keys request.
	 */
	public static int ADD_KEYS_REQUEST = 310;

	/**
	 * The add keys response.
	 */
	public static int ADD_KEYS_RESPONSE = 311;

	/**
	 * The update channel one request.
	 */
	public static int UPDATE_CHANNEL_ONE_REQUEST = 410;

	/**
	 * The update channel one response.
	 */
	public static int UPDATE_CHANNEL_ONE_RESPONSE = 411;

	/**
	 * The update channel two request.
	 */
	public static int UPDATE_CHANNEL_TWO_REQUEST = 420;

	/**
	 * The update channel two response.
	 */
	public static int UPDATE_CHANNEL_TWO_RESPONSE = 421;

	/**
	 * The update channel three request.
	 */
	public static int UPDATE_CHANNEL_THREE_REQUEST = 430;

	/**
	 * The update channel three response.
	 */
	public static int UPDATE_CHANNEL_THREE_RESPONSE = 431;

	/**
	 * The update channel four request.
	 */
	public static int UPDATE_CHANNEL_FOUR_REQUEST = 440;

	/**
	 * The update channel four response.
	 */
	public static int UPDATE_CHANNEL_FOUR_RESPONSE = 441;

	/**
	 * The update channel five request.
	 */
	public static int UPDATE_CHANNEL_FIVE_REQUEST = 450;

	/**
	 * The update channel five response.
	 */
	public static int UPDATE_CHANNEL_FIVE_RESPONSE = 451;

	/**
	 * The close channel request.
	 */
	public static int CLOSE_CHANNEL_REQUEST = 510;

	/**
	 * The close channel response.
	 */
	public static int CLOSE_CHANNEL_RESPONSE = 511;

	public static int WEBSOCKET_OPEN = 610;
	public static int WEBSOCKET_NEW_PAYMENT = 620;
	public static int WEBSOCKET_NEW_SECRET = 630;

}
