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
package network.thunder.server.etc;

// TODO: Auto-generated Javadoc

/**
 * The Class SideConstants.
 */
public class SideConstants {

    /**
     * The Constant DATABASE_CONNECTION2.
     */
    public static final String DATABASE_CONNECTION2 = null;

    /**
     * The Constant DATABASE_CONNECTION_WITHOUT_DB.
     */
    public static final String DATABASE_CONNECTION_WITHOUT_DB = null;

    /**
     * The database connection.
     */
    public static String DATABASE_CONNECTION = "jdbc:mysql://localhost/bitnet?user=root&password=0000&rewriteBatchedStatements=true&autoReconnect=true" + "&allowMultiQueries=true";

    /**
     * The runs on server.
     */
    public static boolean RUNS_ON_SERVER = true;

    /**
     * The wallet file.
     */
    public static String WALLET_FILE = "wallet";

    /**
     * The KE y_ b58.
     */
    public static String KEY_B58 = "tprv8dBsuaYzXZKmPEHjg5xvbD81ghiKMjZqF3UGANYXGofwhD4B375XGiEN8Bqhfv6WADnYKje3GwciwbcZ1GYLHrcDRLmtPTXNNMZnqcZFGqF";

    public static boolean DEBUG = false;
}
