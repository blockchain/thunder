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

public class SideConstants {
	public static String WALLET_FILE = "client_wallet";
	public static String DATABASE_CONNECTION_SERVER = "jdbc:mysql://localhost/bitnet?user=root&password=0000&rewriteBatchedStatements=true";
	public static String DATABASE_CONNECTION = "jdbc:h2:~/bitnet_client_t2;MODE=MySQL;MVCC=true";
	public static String DATABASE_CONNECTION2 = "jdbc:h2:~/bitnet_client2_t2;MODE=MySQL;MVCC=true";

	public static String getDatabaseConnection(int id)  {
		return "jdbc:h2:./thunder_wallet_db_"+id+"_t2;MODE=MySQL;MVCC=true";
	}

	
	
	
	public static String DATABASE_CONNECTION_WITHOUT_DB = "jdbc:h2:~/bitnet_client_t2;MODE=MySQL;MVCC=true";
	
	
	public static boolean RUNS_ON_SERVER = false;
	
	public static String KEY_B58 = "tprv8dBsuaYzXZKmVDXkrxFBVyWFrZYue4dSVsgBFwBiZoALMkxeVT5ENS8D7kt2GnhGpvWn5mVjC8JXDNdN1d5XJDC7wvQmdzmwqHZ65ZrrTiD";

}
