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

public class Type {

    public static int FAILURE = 000;
    public static int ESTABLISH_CHANNEL_ONE_REQUEST = 110;
    public static int ESTABLISH_CHANNEL_ONE_RESPONSE = 111;
    public static int ESTABLISH_CHANNEL_TWO_REQUEST = 120;
    public static int ESTABLISH_CHANNEL_TWO_RESPONSE = 121;
    public static int ESTABLISH_CHANNEL_THREE_REQUEST = 130;
    public static int ESTABLISH_CHANNEL_THREE_RESPONSE = 131;

    public static int SEND_PAYMENT_ONE_REQUEST = 210;
    public static int SEND_PAYMENT_ONE_RESPONSE = 211;
    public static int SEND_PAYMENT_TWO_REQUEST = 220;
    public static int SEND_PAYMENT_TWO_RESPONSE = 221;
    public static int SEND_PAYMENT_THREE_REQUEST = 230;
    public static int SEND_PAYMENT_THREE_RESPONSE = 231;
    public static int SEND_PAYMENT_FOUR_REQUEST = 240;
    public static int SEND_PAYMENT_FOUR_RESPONSE = 241;

    public static int ADD_KEYS_REQUEST = 310;
    public static int ADD_KEYS_RESPONSE = 311;

    public static int UPDATE_CHANNEL_ONE_REQUEST = 410;
    public static int UPDATE_CHANNEL_ONE_RESPONSE = 411;
    public static int UPDATE_CHANNEL_TWO_REQUEST = 420;
    public static int UPDATE_CHANNEL_TWO_RESPONSE = 421;
    public static int UPDATE_CHANNEL_THREE_REQUEST = 430;
    public static int UPDATE_CHANNEL_THREE_RESPONSE = 431;
    public static int UPDATE_CHANNEL_FOUR_REQUEST = 440;
    public static int UPDATE_CHANNEL_FOUR_RESPONSE = 441;
    public static int UPDATE_CHANNEL_FIVE_REQUEST = 450;
    public static int UPDATE_CHANNEL_FIVE_RESPONSE = 451;

    public static int CLOSE_CHANNEL_REQUEST = 510;
    public static int CLOSE_CHANNEL_RESPONSE = 511;

    public static int WEBSOCKET_OPEN = 610;
    public static int WEBSOCKET_NEW_PAYMENT = 620;

}
