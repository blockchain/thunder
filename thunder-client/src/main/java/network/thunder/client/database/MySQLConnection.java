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
package network.thunder.client.database;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import javax.sql.DataSource;

import network.thunder.client.api.ThunderContext;
import network.thunder.client.communications.Message;
import network.thunder.client.communications.objects.EstablishChannelRequestOne;
import network.thunder.client.database.objects.Channel;
import network.thunder.client.database.objects.Key;
import network.thunder.client.database.objects.KeyWrapper;
import network.thunder.client.database.objects.Output;
import network.thunder.client.database.objects.Payment;
import network.thunder.client.database.objects.TransactionWrapper;
import network.thunder.client.database.objects.KeyWrapper.IKey;
import network.thunder.client.etc.Constants;
import network.thunder.client.etc.KeyDerivation;
import network.thunder.client.etc.PerformanceLogger;
import network.thunder.client.etc.ScriptRunner;
import network.thunder.client.etc.ScriptTools;
import network.thunder.client.etc.SideConstants;
import network.thunder.client.etc.Tools;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.ECKey.ECDSASignature;
import org.bitcoinj.core.ScriptException;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Transaction.SigHash;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicHierarchy;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;

import com.mchange.v2.c3p0.ComboPooledDataSource;


/**
 * 
 * @author PC
 *
 */
@SuppressWarnings("ALL")
public class MySQLConnection {


	public static DataSource getDataSource() throws PropertyVetoException {
		ComboPooledDataSource cpds = new ComboPooledDataSource();
		cpds.setDriverClass( "com.mysql.jdbc.Driver" ); //loads the jdbc driver            
		cpds.setJdbcUrl( SideConstants.DATABASE_CONNECTION );
                                
			
		// the settings below are optional -- c3p0 can work with defaults
		cpds.setMinPoolSize(2);                                     
		cpds.setAcquireIncrement(5);
		cpds.setMaxPoolSize(8);

		
		return cpds;
	}
	
	public static DataSource getDataSource(int id) throws PropertyVetoException {
		ComboPooledDataSource cpds = new ComboPooledDataSource();
		cpds.setDriverClass( "com.mysql.jdbc.Driver" ); //loads the jdbc driver   
		if(id==1)
			cpds.setJdbcUrl( SideConstants.DATABASE_CONNECTION );
		if(id==2)
			cpds.setJdbcUrl( SideConstants.DATABASE_CONNECTION2 );
              
			
		// the settings below are optional -- c3p0 can work with defaults
		cpds.setMinPoolSize(2);                                     
		cpds.setAcquireIncrement(5);
		cpds.setMaxPoolSize(8);
		
		return cpds;
	}
	
	public static void buildDatabaseOld(Connection conn) throws IOException, SQLException  {
//		ComboPooledDataSource cpds = new ComboPooledDataSource();
//		cpds.setDriverClass( "com.mysql.jdbc.Driver" ); //loads the jdbc driver            
//		cpds.setJdbcUrl( SideConstants.DATABASE_CONNECTION_WITHOUT_DB );
//                                
//			
//		// the settings below are optional -- c3p0 can work with defaults
//		cpds.setMinPoolSize(2);                                     
//		cpds.setAcquireIncrement(5);
//		cpds.setMaxPoolSize(8);
		
		ScriptRunner scriptRunner = new ScriptRunner(conn, false, true);
		FileReader reader = new FileReader(new File("create_tables_client.sql"));
		scriptRunner.runScript(reader);
		
		
		
	}
	
	public static void buildDatabase(Connection conn) throws IOException, SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException  {
//		ComboPooledDataSource cpds = new ComboPooledDataSource();
//		cpds.setDriverClass( "com.mysql.jdbc.Driver" ); //loads the jdbc driver            
//		cpds.setJdbcUrl( SideConstants.DATABASE_CONNECTION_WITHOUT_DB );
//                                
//			
//		// the settings below are optional -- c3p0 can work with defaults
//		cpds.setMinPoolSize(2);                                     
//		cpds.setAcquireIncrement(5);
//		cpds.setMaxPoolSize(8);
		
		try {
			
		
		ScriptRunner scriptRunner = new ScriptRunner(conn, false, true);
		InputStream is = MySQLConnection.class.getClassLoader().getResourceAsStream("sql.sql");
//		FileReader reader = new FileReader(new File("create_tables_client.sql"));
		scriptRunner.runScript(new InputStreamReader(is));
		
		} catch(Exception e) {
			Class.forName("org.h2.Driver").newInstance();
			ScriptRunner scriptRunner = new ScriptRunner(conn, false, true);
			FileReader reader = new FileReader(new File("sql.sql"));
			scriptRunner.runScript(reader);
			conn.commit();

		}
		
	}
	
	public static void resetToBackup() throws PropertyVetoException, SQLException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
//		ComboPooledDataSource cpds = new ComboPooledDataSource();
//		cpds.setDriverClass( "com.mysql.jdbc.Driver" ); //loads the jdbc driver            
//		cpds.setJdbcUrl( SideConstants.DATABASE_CONNECTION_WITHOUT_DB );
//                                
//			
//		// the settings below are optional -- c3p0 can work with defaults
//		cpds.setMinPoolSize(2);                                     
//		cpds.setAcquireIncrement(5);
//		cpds.setMaxPoolSize(8);
		
//		conn.setAutoCommit(false);
		if(SideConstants.RUNS_ON_SERVER) {
			Connection conn = MySQLConnection.getInstance();
			ScriptRunner scriptRunner = new ScriptRunner(conn, false, true);
			FileReader reader = new FileReader(new File("sql.sql"));
			scriptRunner.runScript(reader);
		} else {
			Class.forName("org.h2.Driver").newInstance();
			Connection conn =  DriverManager.getConnection(SideConstants.DATABASE_CONNECTION);
			ScriptRunner scriptRunner = new ScriptRunner(conn, false, true);
			FileReader reader = new FileReader(new File("sql.sql"));
			scriptRunner.runScript(reader);
			conn.close();
			
			conn =  DriverManager.getConnection(SideConstants.DATABASE_CONNECTION2);
			scriptRunner = new ScriptRunner(conn, false, true);
			reader = new FileReader(new File("sql.sql"));
			scriptRunner.runScript(reader);
			conn.close();
			
//			Class.forName("com.mysql.jdbc.Driver").newInstance();
//			conn =  DriverManager.getConnection(SideConstants.DATABASE_CONNECTION_SERVER);
//			scriptRunner = new ScriptRunner(conn, false, true);
//			reader = new FileReader(new File("backup.sql"));
//			scriptRunner.runScript(reader);
//			conn.close();
		}
		
	}
	
	
	
	
	
	
	public static void addKey(Connection conn, Key key, int channelId, boolean ourKey) throws SQLException {
		

		PreparedStatement stmt = null;
		
		try {
		
			stmt = conn.prepareStatement("INSERT INTO storedkeys VALUES(?,?,?,?,?,?,?,?,?,?)");
			stmt.setString(1, null);
			stmt.setInt(2, channelId);
			stmt.setInt(3, Tools.boolToInt(ourKey));
			stmt.setString(4, key.publicKey);
			stmt.setString(5, key.privateKey);
			stmt.setInt(6, 0);
			stmt.setInt(7, 0);
			stmt.setInt(8, 0);
			stmt.setInt(9, key.depth);
			stmt.setInt(10, key.child);
			stmt.execute();
			
			stmt.close();
			conn.commit();
		
		} finally {
			if(stmt != null) stmt.close();
		}

	}
	
	public static void addKey(Connection conn, ArrayList<Key> keyList, int channelId, boolean ourKey) throws SQLException {
		
		if(!ourKey) {
			for(Key key : keyList) {
				key.privateKey = null;
			}
		}

		PreparedStatement stmt = null;
		
		try {
		

	
			stmt = conn.prepareStatement("INSERT INTO storedkeys VALUES(?,?,?,?,?,?,?,?,?,?)");
			for(Key key : keyList) {
				stmt.setString(1, null);
				stmt.setInt(2, channelId);
				stmt.setInt(3, Tools.boolToInt(ourKey));
				stmt.setString(4, key.publicKey);
				stmt.setString(5, key.privateKey);
				stmt.setInt(6, 0);
				stmt.setInt(7, 0);
				stmt.setInt(8, 0);
				stmt.setInt(9, key.depth);
				stmt.setInt(10, key.child);
				stmt.addBatch();
			}
			
			stmt.executeBatch();
			conn.commit();

		} finally {
			if(stmt != null) stmt.close();
		}


	}
	
	public static void updateKey(Connection conn, Channel channel, Key key, boolean used, boolean exposed) throws SQLException {

		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement("UPDATE storedkeys SET priv_key=?, used=?, exposed=? WHERE channel_id=? AND pub_key=?");
			stmt.setString(1, key.privateKey);
			stmt.setInt(2, Tools.boolToInt(used));
			stmt.setInt(3, Tools.boolToInt(exposed));
			stmt.setInt(4, channel.getId());
			stmt.setString(5, key.publicKey);

			stmt.execute();
		} finally {
			if(stmt != null) stmt.close();
		}
	}
	
	public static void updateKey(Connection conn, Channel channel, ArrayList<Key> key, boolean used, boolean exposed) throws SQLException {

		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement("UPDATE storedkeys SET priv_key=?, used=?, exposed=? WHERE channel_id=? AND pub_key=?");
			for(Key k : key) {
				if(k.child>0) {
					stmt.setString(1, k.privateKey);
					stmt.setInt(2, Tools.boolToInt(used));
					stmt.setInt(3, Tools.boolToInt(exposed));
					stmt.setInt(4, channel.getId());
					stmt.setString(5, k.publicKey);
					
//					System.out.println(k.publicKey);
//					System.out.println(k.privateKey);
					
					stmt.addBatch();
				}
			}
	
			stmt.executeBatch();
			conn.commit();
		} finally {
			if(stmt != null) stmt.close();
		}
	}
	

	/**
	 * Insert a output into the database.
	 * Will get called every time a new payment is received over the KeyChain.
	 * 
	 * 
	 * @param conn
	 * @param output
	 * @throws SQLException
	 */
	public static void addOutput(Connection conn, Output output) throws SQLException {
		PreparedStatement stmt = null;
		try {
	
			stmt = conn.prepareStatement("INSERT INTO outputs (transaction_hash, vout, value, private_key, timestamp_locked, transaction_output) values (?, ?, ?, ?, ?, ?)");
			
			stmt.setString(1, output.getHash());
			stmt.setInt(2, output.getVout());
			stmt.setLong(3, output.getValue());
			stmt.setString(4, output.getPrivateKey());
			stmt.setInt(5, output.getLock());
			stmt.setString(6, Tools.byteToString(output.getTransactionOutput().bitcoinSerialize()));

			stmt.executeUpdate();

			stmt.close();
		} finally {
			if(stmt != null) stmt.close();
		}
	}

	public static int addTransaction(Connection conn, TransactionWrapper wrapper) throws SQLException {
		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement("INSERT INTO transactions (id, hash, channel_id, payment_id, data) values (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			
			stmt.setString(1, null);
			stmt.setString(2, wrapper.getHash());
			stmt.setInt(3, wrapper.getChannelId());
			stmt.setInt(4, wrapper.getId());
			stmt.setBytes(5, wrapper.getData());
			
			
		    stmt.executeUpdate();
		    
		    ResultSet rs = stmt.getGeneratedKeys();
		    rs.first();
		    int id = rs.getInt(1);
		    rs.close();
			return id;
	
		} finally {
			if(stmt != null) stmt.close();
		}
	}
	
	public static int addOrUpdateTransaction(Connection conn, TransactionWrapper wrapper) throws SQLException {
		PreparedStatement stmt = null;
		try {
			if(wrapper.getId() == 0) {
				stmt = conn.prepareStatement("INSERT INTO transactions (id, hash, channel_id, payment_id, data) values (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
				
				stmt.setString(1, null);
				stmt.setString(2, wrapper.getHash());
				stmt.setInt(3, wrapper.getChannelId());
				stmt.setInt(4, wrapper.getId());
				stmt.setBytes(5, wrapper.getData());
				
				
				
			    stmt.executeUpdate();
			    
			    ResultSet rs = stmt.getGeneratedKeys();
			    rs.first();
			    int id = rs.getInt(1);
		
			    stmt.close();
				
				return id;
			} else {
				stmt = conn.prepareStatement("UPDATE transactions SET hash=?, channel_id=?, payment_id=?, data=? WHERE id=?");
				
				stmt.setString(1, wrapper.getHash());
				stmt.setInt(2, wrapper.getChannelId());
				stmt.setInt(3, wrapper.getId());
				stmt.setBytes(4, wrapper.getData());
				stmt.setInt(5, wrapper.getId());
				
				
			    stmt.executeUpdate();
			    return wrapper.getId();
			}
		} finally {
			if(stmt != null) stmt.close();
		}
	}
	
	public static int addPayment(Connection conn, Payment payment) throws SQLException {
		PreparedStatement stmt = null;
		try {
			payment.updateTransactionsToDatabase(conn);
			stmt = conn.prepareStatement("INSERT INTO payments values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);

            stmt.setString(1, null);
			stmt.setInt(2, payment.getChannelIdSender());
			stmt.setInt(3, payment.getChannelIdReceiver());
			stmt.setLong(4, payment.getAmount());
			stmt.setInt(5, payment.getPhase());
			stmt.setInt(6, Tools.boolToInt(payment.isIncludeInSenderChannel()));
			stmt.setInt(7, Tools.boolToInt(payment.isIncludeInReceiverChannel()));
			stmt.setString(8, payment.getSecretHash());
			stmt.setString(9, payment.getSecret());
			stmt.setInt(10, payment.getSettlementTxSenderID());
			stmt.setInt(11, payment.getSettlementTxSenderTempID());
			stmt.setInt(12, payment.getRefundTxSenderID());
			stmt.setInt(13, payment.getRefundTxSenderTempID());
			stmt.setInt(14, payment.getAddTxSenderID());
			stmt.setInt(15, payment.getAddTxSenderTempID());
			stmt.setInt(16, payment.getSettlementTxReceiverID());
			stmt.setInt(17, payment.getSettlementTxReceiverTempID());
			stmt.setInt(18, payment.getRefundTxReceiverID());
			stmt.setInt(19, payment.getRefundTxReceiverTempID());
			stmt.setInt(20, payment.getAddTxReceiverID());
			stmt.setInt(21, payment.getAddTxReceiverTempID());
			stmt.setInt(22, payment.getTimestampCreated());
			stmt.setInt(23, payment.getTimestampAddedToReceiver());		
			stmt.setInt(24, payment.getTimestampSettled());

			stmt.executeUpdate();

            ResultSet generatedKeys = stmt.getGeneratedKeys();
            int id = 0;
            if(generatedKeys.first()) {
                id = generatedKeys.getInt(1);
                payment.setId(id);
                return id;
            }

            throw new SQLException("No id returned for addPayment");
		} finally {
			if(stmt != null) stmt.close();
		}
	}
	
	public static void updatePayment(Connection conn, Payment payment) throws SQLException {
		PreparedStatement stmt = null;
		try {
			payment.updateTransactionsToDatabase(conn);
			stmt = conn.prepareStatement("UPDATE payments SET channel_id_sender=?, channel_id_receiver=?, amount=?, phase=?, secret=?, settlement_tx_sender=?, refund_tx_sender=?, settlement_tx_receiver=?, refund_tx_receiver=?, timestamp_created=?, timestamp_settled=?, include_in_sender_channel=?, include_in_receiver_channel=?, add_tx_sender=?, add_tx_receiver=?, settlement_tx_sender_temp=?, refund_tx_sender_temp=?, add_tx_sender_temp=?, settlement_tx_receiver_temp=?, refund_tx_receiver_temp=?, add_tx_receiver_temp=?, timestamp_added_to_receiver=? WHERE id=?");
			
	
			stmt.setInt(1, payment.getChannelIdSender());
			stmt.setInt(2, payment.getChannelIdReceiver());
			stmt.setLong(3, payment.getAmount());
			stmt.setInt(4, payment.getPhase());
			stmt.setString(5, payment.getSecret());
			stmt.setInt(6, payment.getSettlementTxSenderID());
			stmt.setInt(7, payment.getRefundTxSenderID());
			stmt.setInt(8, payment.getSettlementTxReceiverID());
			stmt.setInt(9, payment.getRefundTxReceiverID());
			stmt.setInt(10, payment.getTimestampCreated());
			stmt.setInt(11, payment.getTimestampSettled());	
			
			stmt.setInt(12, Tools.boolToInt(payment.isIncludeInSenderChannel()));
			stmt.setInt(13, Tools.boolToInt(payment.isIncludeInReceiverChannel()));
			
			stmt.setInt(14, payment.getAddTxSenderID());
			stmt.setInt(15, payment.getAddTxReceiverID());
		
			stmt.setInt(16, payment.getSettlementTxSenderTempID());
			stmt.setInt(17, payment.getRefundTxSenderTempID());
			stmt.setInt(18, payment.getAddTxSenderTempID());
			stmt.setInt(19, payment.getSettlementTxReceiverTempID());
			stmt.setInt(20, payment.getRefundTxReceiverTempID());
			stmt.setInt(21, payment.getAddTxReceiverTempID());
			stmt.setInt(22, payment.getTimestampAddedToReceiver());

			
			stmt.setInt(23, payment.getId());
					
		    stmt.executeUpdate();
		} finally {
			if(stmt != null) stmt.close();
		}
	}
	
	public static void updatePaymentRefunds(Connection conn, Channel channel) throws SQLException {
		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement("UPDATE payments SET phase=6 WHERE phase=5 AND channel_id_receiver=?");
			
	
			stmt.setInt(1, channel.getId());
			stmt.executeUpdate();
		} finally {
			if(stmt != null) stmt.close();
		}
	}
	
	
	public static void updatePayment(Connection conn, ArrayList<Payment> paymentList) throws SQLException {
		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement("UPDATE payments SET channel_id_sender=?, channel_id_receiver=?, amount=?, phase=?, secret=?, settlement_tx_sender=?, refund_tx_sender=?, settlement_tx_receiver=?, refund_tx_receiver=?, timestamp_created=?, timestamp_settled=?, include_in_sender_channel=?, include_in_receiver_channel=?, add_tx_sender=?, add_tx_receiver=?, settlement_tx_sender_temp=?, refund_tx_sender_temp=?, add_tx_sender_temp=?, settlement_tx_receiver_temp=?, refund_tx_receiver_temp=?, add_tx_receiver_temp=?, timestamp_added_to_receiver=? WHERE id=?");
	
			for(Payment payment : paymentList) {
				payment.updateTransactionsToDatabase(conn);
				
		
				stmt.setInt(1, payment.getChannelIdSender());
				stmt.setInt(2, payment.getChannelIdReceiver());
				stmt.setLong(3, payment.getAmount());
				stmt.setInt(4, payment.getPhase());
				stmt.setString(5, payment.getSecret());
				stmt.setInt(6, payment.getSettlementTxSenderID());
				stmt.setInt(7, payment.getRefundTxSenderID());
				stmt.setInt(8, payment.getSettlementTxReceiverID());
				stmt.setInt(9, payment.getRefundTxReceiverID());
				stmt.setInt(10, payment.getTimestampCreated());
				stmt.setInt(11, payment.getTimestampSettled());	
				
				stmt.setInt(12, Tools.boolToInt(payment.isIncludeInSenderChannel()));
				stmt.setInt(13, Tools.boolToInt(payment.isIncludeInReceiverChannel()));
				
				stmt.setInt(14, payment.getAddTxSenderID());
				stmt.setInt(15, payment.getAddTxReceiverID());
			
				stmt.setInt(16, payment.getSettlementTxSenderTempID());
				stmt.setInt(17, payment.getRefundTxSenderTempID());
				stmt.setInt(18, payment.getAddTxSenderTempID());
				stmt.setInt(19, payment.getSettlementTxReceiverTempID());
				stmt.setInt(20, payment.getRefundTxReceiverTempID());
				stmt.setInt(21, payment.getAddTxReceiverTempID());
				stmt.setInt(22, payment.getTimestampAddedToReceiver());
		
				
				stmt.setInt(23, payment.getId());
				stmt.addBatch();
			}
			
			stmt.executeBatch();
			conn.commit();


		} finally {
			if(stmt != null) stmt.close();
		}
	}
	
	public static ArrayList<Payment> getPaymentsIncludedInChannel(Connection conn, int channelId) throws SQLException {
		PreparedStatement stmt = null;
		try {
			ArrayList<Payment> list = new ArrayList<Payment>();
	
			stmt = conn.prepareStatement("SELECT * FROM payments WHERE (channel_id_sender=? AND include_in_sender_channel=1) OR (channel_id_receiver=? AND include_in_receiver_channel=1) ORDER BY id ASC");
			stmt.setInt(1, channelId);
			stmt.setInt(2, channelId);
			ResultSet results = stmt.executeQuery();
			
			if(results.first()) {
				
				while(!results.isAfterLast() ) {
					
					Payment payment = new Payment(results);
					
					if(payment.getChannelIdReceiver() == channelId) {
						payment.paymentToServer = false;
					} else {
						payment.paymentToServer = true;
					}
					
					list.add(payment);
						
					
					results.next();
				}
				
			}
			results.close();

			return list;
	
		} finally {
			if(stmt != null) stmt.close();
		}		
	}
	
	public static ArrayList<Payment> getPaymentsIncludedInChannelWithNoSecret(Connection conn, int channelId) throws SQLException {
		PreparedStatement stmt = null;
		try {
			ArrayList<Payment> list = new ArrayList<Payment>();
	
			stmt = conn.prepareStatement("SELECT * FROM payments WHERE ( ( (channel_id_sender=? AND include_in_sender_channel=1 AND phase!=6) OR (channel_id_receiver=? AND include_in_receiver_channel=1 AND phase!=5 AND phase!=6) ) AND secret IS NULL) ORDER BY id ASC");
			stmt.setInt(1, channelId);
			stmt.setInt(2, channelId);
			ResultSet results = stmt.executeQuery();
			
			if(results.first()) {
				
				while(!results.isAfterLast() ) {
					
					Payment payment = new Payment(results);

					if(payment.getChannelIdReceiver() == channelId) {
						payment.paymentToServer = false;
					} else {
						payment.paymentToServer = true;
					}
					
					list.add(payment);
						
					
					results.next();
				}
				
			}
			results.close();

			return list;
	
		} finally {
			if(stmt != null) stmt.close();
		}		
	}
	
	public static ArrayList<Payment> getPaymentsForUpdatingChannelOrdered(Connection conn, int channelId, int amount) throws SQLException {
		ArrayList<Payment> paymentList = MySQLConnection.getPaymentsIncludedInChannelWithNoSecret(conn, channelId);
		paymentList.addAll(MySQLConnection.getPaymentsIncludedInChannelWithPaymentsNotAddedYet(conn, channelId, (amount - paymentList.size())));
		
		paymentList.sort(new Comparator<Payment>() {
			@Override
			public int compare(Payment arg0, Payment arg1) {
				return arg0.getId()-arg1.getId();
			}
		});
		return paymentList;

	}
	
	public static ArrayList<Payment> getPaymentsIncludedInChannelWithPaymentsNotAddedYet(Connection conn, int channelId, int amount) throws SQLException {
		PreparedStatement stmt = null;
		try {
			ArrayList<Payment> list = new ArrayList<Payment>();
	
			stmt = conn.prepareStatement("SELECT * FROM payments WHERE ( (channel_id_receiver=? AND include_in_receiver_channel=0) AND phase != 6 AND secret IS NULL) ORDER BY id ASC");
			stmt.setInt(1, channelId);
			ResultSet results = stmt.executeQuery();
			
			if(results.first()) {
				
				while(!results.isAfterLast() ) {
					
					Payment payment = new Payment(results);
					
					if(payment.getChannelIdReceiver() == channelId) {
						payment.paymentToServer = false;
					} else {
						payment.paymentToServer = true;
					}
					
					list.add(payment);
						
					
					results.next();
				}
				
			}
			results.close();
			
			return list;
	
		} finally {
			if(stmt != null) stmt.close();
		}		
	}

	
	public static ArrayList<Payment> getPaymentsIncludedInChannelWithMostRecent(Connection conn, int channelId) throws SQLException {
		ArrayList<Payment> list = getPaymentsIncludedInChannel(conn, channelId);
		list.add(getPaymentMostRecentSent(conn, channelId));
		return list;
	}
	
	public static Payment getPayment(Connection conn, String secretHash, int channelId) throws SQLException {
		PreparedStatement stmt = null;
		try {

			stmt = conn.prepareStatement("SELECT * FROM payments WHERE secret_hash=?");
			stmt.setString(1, secretHash);

			ResultSet results = stmt.executeQuery();
			Payment payment = null;
			if(results.first()) {
								
				payment = new Payment(results);
				
				if(payment.getChannelIdReceiver() == channelId) {
					payment.paymentToServer = false;
				} else {
					payment.paymentToServer = true;
				}					
			}
			
			results.close();
			return payment;
		} finally {
			if(stmt != null) stmt.close();
		}		
	}
	
	public static Payment getPaymentMostRecentSent(Connection conn, int channelId) throws SQLException {
		PreparedStatement stmt = null;
		try {
	
			stmt = conn.prepareStatement("SELECT * FROM payments WHERE channel_id_sender=? AND phase=0 ORDER BY id DESC LIMIT 1");
			stmt.setInt(1, channelId);
			
			ResultSet results = stmt.executeQuery();
			Payment payment = null;
			
			if(results.first()) {
								
				payment = new Payment(results);
				payment.paymentToServer = true;
			}
				
					
				results.close();
				return payment;
		} finally {
			if(stmt != null) stmt.close();
		}		
	}
	
	
	
	public static void cleanUpDatabase(Connection conn) throws SQLException {
		Statement stmt = null;
		try {

			stmt = conn.createStatement();
			String query = "SELECT * FROM channels WHERE (is_ready=0 AND establish_phase<4 AND timestamp_open<"+(Tools.currentTime()-Constants.MAX_CHANNEL_CREATION_TIME)+") OR (timestamp_close< " + (Tools.currentTime()-Constants.MAX_CHANNEL_KEEP_TIME_AFTER_CLOSED) + ")";
			ResultSet results = stmt.executeQuery(query);
			
			ArrayList<Channel> list = new ArrayList<Channel>();
			
			if(results.first()) {
				while(!results.isAfterLast()) {
					list.add(new Channel(results));
					results.next();
				}
			}
			
			results.close();
			
			for(Channel channel : list ) {
					
					query = "DELETE FROM transactions WHERE channel_id='"+channel.getId()+"'";
					stmt.execute(query);
					
					query = "DELETE FROM messages WHERE pubkey='"+channel.getId()+"'";
					stmt.execute(query);
					
					query = "DELETE FROM channels WHERE pub_key_client='"+channel.getId()+"'";
					stmt.execute(query);
	
			}
		} finally {
			if(stmt != null) stmt.close();
		}
	}
	

	
	
	
	/**
	 *  Create a new channel based on the request MessageOneRequest
	 *  Will also create a new master key for us to use in this channel
	 * 
	 * @param conn
	 * @param message
	 * @param pubkeyChannel
	 * @return
	 * @throws SQLException
	 */
	public static Channel createNewChannel(Connection conn, EstablishChannelRequestOne message, int timeStampOpen, String changeAddressServer, String changeAddressClient) throws SQLException {
		
		PreparedStatement stmt = null;
		try {
			
			stmt = conn.prepareStatement("INSERT INTO channels (pub_key_client, pub_key_server, change_address_server, change_address_client , master_priv_key_server, timestamp_open, timestamp_close, establish_phase, is_ready, key_chain_depth, key_chain_child) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			
			DeterministicKey key = KeyDerivation.getMasterKey(MySQLConnection.getKeyCount(conn));
			
			String masterKey = key.serializePrivB58(Constants.getNetwork());
	//		String masterKey = key.serializePrivB58();
			
			stmt.setString(1, message.pubKey);
			stmt.setString(2, Tools.byteToString(key.getPubKey()));
			stmt.setString(3, changeAddressServer);
			stmt.setString(4, changeAddressClient);
			stmt.setString(5, masterKey);
			stmt.setInt(6, timeStampOpen);
			stmt.setInt(7, timeStampOpen + 60 * 60 * 24 * message.timeInDays);
			stmt.setInt(8, 1);
			stmt.setInt(9, 0);
			stmt.setInt(10, ( message.timeInDays * 24 * 60 * 60 / Constants.TIMEFRAME_PER_KEY_DEPTH ) );
			stmt.setInt(11, 0);
			
	//		System.out.println(stmt.toString());
	//	    System.out.println(stmt.executeUpdate());
			stmt.executeUpdate();
			
			conn.commit();
			
			Channel channel = MySQLConnection.getChannel(conn, message.pubKey);
		    
//		    Channel channel = new Channel();
//		    channel.setMasterPrivateKeyServer(masterKey);
//		    channel.setPubKeyClient(message.pubKey);
//		    channel.setChangeAddressClient(changeAddressClient);
//		    channel.setChangeAddressServer(changeAddressServer);
//		    channel.setKeyChainChild(0);
//		    channel.setKeyChainDepth(( (int) (message.timeInDays * 24 * 60 * 60 / Constants.TIMEFRAME_PER_KEY_DEPTH ) ));
//		    conn.commit();
    
	
		    return channel;
		} finally {
			if(stmt != null) stmt.close();
		}
	}
	
	public static boolean checkKey(Connection conn, Channel channel, String pubkey, boolean checkForPrivate) throws Exception  {
		PreparedStatement stmt = null;
		try {
			String sql = "";
			if(checkForPrivate) {
				sql = "SELECT * FROM storedkeys WHERE (channel_id=? AND pub_key=? AND used=0 AND exposed=0 AND priv_key IS NOT NULL) FOR UPDATE";
			} else {
				sql = "SELECT * FROM storedkeys WHERE (channel_id=? AND pub_key=? AND used=0 AND exposed=0 AND priv_key IS NULL) FOR UPDATE";
			}
			
			conn.commit();
			stmt = conn.prepareStatement(sql);
	
			stmt.setInt(1, channel.getId());
			stmt.setString(2, pubkey);
					
			ResultSet set = stmt.executeQuery();
			
			if(!set.first()) {
				System.out.println("No key found...: "+stmt.toString());
				set.close();
				return false;
			}
			
			int id = set.getInt("id");
			
			set.close();
			stmt.close();
				
			
			stmt = conn.prepareStatement("UPDATE storedkeys SET used=1 WHERE id=?");
			stmt.setInt(1, id);
			stmt.execute();
			
			conn.commit();
			return true;
		} finally {
			if(stmt != null) stmt.close();
		}
	}
	
	public static KeyWrapper getKeysPubOnly(Connection conn, Channel channel, boolean keyFromThisSide) throws Exception  {
		PreparedStatement stmt = null;
		try {
			String sql = "";
			if(keyFromThisSide) {
				sql = "SELECT id, pub_key FROM storedkeys WHERE (channel_id=? AND used=0 AND exposed=0 AND priv_key IS NOT NULL) ORDER BY key_chain_depth ASC, key_chain_child DESC FOR UPDATE";
			} else {
				sql = "SELECT id, pub_key FROM storedkeys WHERE (channel_id=? AND used=0 AND exposed=0 AND priv_key IS NULL) ORDER BY key_chain_depth ASC, key_chain_child DESC FOR UPDATE";
			}
	
			stmt = conn.prepareStatement(sql);
					
			stmt.setInt(1, channel.getId());
			ResultSet set = stmt.executeQuery();
			
			KeyWrapper keyWrapper = new KeyWrapper();
						
			if(!set.first()) {
				System.out.println("No Key found for: "+stmt.toString());
				conn.commit();
				set.close();
				return null;
			}
			
			while(!set.isAfterLast()) {
				keyWrapper.addKey(set.getInt("id"), set.getString("pub_key"));
				set.next();
			}
			set.close();
			return keyWrapper;
		} finally {
			if(stmt != null) stmt.close();
		}
	}
	
	public static void setKeysUsed(Connection conn, KeyWrapper keys) throws SQLException {
		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement("UPDATE storedkeys SET used=1 WHERE id=?");
			
			for(IKey k : keys.getKeyList()) {
				if(k.used) {
					stmt.setInt(1, k.id);
					stmt.addBatch();
				}
			}
			stmt.executeBatch();
			conn.commit();
		} finally {
			if(stmt != null) stmt.close();
		}
	}
	
	
	public static String getKey(Connection conn, Channel channel, boolean keyFromThisSide) throws Exception  {
		PreparedStatement stmt = null;
		try {
			String sql = "";
			if(keyFromThisSide) {
				sql = "SELECT id, pub_key FROM storedkeys WHERE (channel_id=? AND used=0 AND exposed=0 AND priv_key IS NOT NULL) ORDER BY key_chain_depth ASC, key_chain_child DESC LIMIT 1 FOR UPDATE";
			} else {
				sql = "SELECT id, pub_key FROM storedkeys WHERE (channel_id=? AND used=0 AND exposed=0 AND priv_key IS NULL) ORDER BY key_chain_depth ASC, key_chain_child DESC LIMIT 1 FOR UPDATE";
			}
			
			
			
			stmt = conn.prepareStatement(sql);
					
			stmt.setInt(1, channel.getId());
			ResultSet set = stmt.executeQuery();
			
			if(!set.first()) {
				System.out.println("No Key found for: "+stmt.toString());
				conn.commit();
				set.close();
				return null;
			}
				
			
			String pubKey = set.getString("pub_key");
			int id = set.getInt("id");
			stmt.close();
			stmt = conn.prepareStatement("UPDATE storedkeys SET used=1 WHERE id=?");
			stmt.setInt(1, id);
			stmt.execute();


			return pubKey;
		} finally {
			if(stmt != null) stmt.close();
		}
	}
	
	
	public static ECKey getKey(Connection conn, Channel channel, String pubKey) throws Exception  {
		PreparedStatement stmt = null;
		try {
			String sql = "SELECT priv_key FROM storedkeys WHERE (channel_id=? AND pub_key=? AND used=1 AND exposed=0 AND priv_key IS NOT NULL)";
	
		
			stmt = conn.prepareStatement(sql);
			stmt.setInt(1, channel.getId());
			stmt.setString(2, pubKey);

			ResultSet set = stmt.executeQuery();
			
			if(!set.first()) {
				System.out.println("getKey return null..: "+stmt.toString());
				set.close();
				return null;
			}
			ECKey key = ECKey.fromPrivate(Tools.stringToByte(set.getString("priv_key")));
			set.close();
			return key;
		} finally {
			if(stmt != null) stmt.close();
		}
	}
	
	public static ECKey getKeyCurrentlyUsed(Connection conn, Channel channel) throws Exception  {
		PreparedStatement stmt = null;
		try {
			String sql = "SELECT priv_key FROM storedkeys WHERE (channel_id=? AND used=1 AND exposed=0 AND owner=1)";
	
		
			stmt = conn.prepareStatement(sql);
			stmt.setInt(1, channel.getId());

			ResultSet set = stmt.executeQuery();
			
			if(!set.first()) {
				System.out.println("getKey return null..: " + stmt.toString());
				set.close();
				return null;
			}
			ECKey key = ECKey.fromPrivate(Tools.stringToByte(set.getString("priv_key")));
			set.close();
			return key;
		} finally {
			if(stmt != null) stmt.close();
		}
	}
	
	public static ArrayList<Key> getKeysOfUsToBeExposed(Connection conn, Channel channel, boolean exposeKeys) throws Exception  {
		PerformanceLogger performanceLogger = new PerformanceLogger();
		PreparedStatement stmt = null;
		ResultSet set = null;
		try {
			String sql;
            sql = "SELECT * FROM storedkeys WHERE (channel_id=? AND exposed=0 AND priv_key IS NOT NULL)";
			/**
			 * Actually we can send also unused keys, as we produce new keys with every connection anyways..
			 */
//			sql = "SELECT * FROM storedkeys WHERE (channel_id=? AND used=1 AND exposed=0 AND priv_key IS NOT NULL)";
	
	
		
			stmt = conn.prepareStatement(sql);
			stmt.setInt(1, channel.getId());

			set = stmt.executeQuery();
	
			
			if(!set.first())
				return null;
			
			ArrayList<Key> keysAll = new ArrayList<Key>();
			
			ArrayList<Key> keysToBeExposed = new ArrayList<Key>();
			ArrayList<Key> keysToKeep = new ArrayList<Key>();
	
			
			ArrayList<String> currentKeys = ScriptTools.getPubKeysOfChannel(channel, SideConstants.RUNS_ON_SERVER, true);
	
	
			while(!set.isAfterLast()) {
				
				String pub = set.getString("pub_key");
				boolean expose = true;
				for(String s : currentKeys) {
					if(pub.equals(s)) {
						expose = false;
	//					break;
					}
				}
				
				Key key = new Key();
				key.child = set.getInt("key_chain_child");
				key.depth = set.getInt("key_chain_depth");
				key.publicKey = set.getString("pub_key");
				key.privateKey = set.getString("priv_key");
				
				keysAll.add(key);
				
				if(expose) {
					keysToBeExposed.add(key);
				} else {
					keysToKeep.add(key);
				}
				
				
				set.next();
			}
					
			int highestDepthToKeep = 0;
			int lowestDepthToExpose = 999999;
			
			stmt.close();
			
			stmt = conn.prepareStatement("UPDATE storedkeys SET exposed=1 WHERE (channel_id=? AND pub_key=?)");
	
			
			for(Key k : keysToBeExposed) {
				stmt.setInt(1, channel.getId());
				stmt.setString(2, k.publicKey);
				stmt.addBatch();
				
				if(lowestDepthToExpose > k.depth)
					lowestDepthToExpose = k.depth;
			}
			if(exposeKeys)
				stmt.executeBatch();
			
			for(Key k : keysToKeep) {
				if(highestDepthToKeep < k.depth)
					highestDepthToKeep = k.depth;
			}
			
			if(highestDepthToKeep<lowestDepthToExpose) {
				ArrayList<Key> keysToBeExposedNew = new ArrayList<Key>();
				String masterKey;
				if(SideConstants.RUNS_ON_SERVER) {
					masterKey = channel.getMasterPrivateKeyServer();
				} else {
					masterKey = channel.getMasterPrivateKeyClient();
				}
				
				DeterministicKey masterKeyToExpose = KeyDerivation.calculateKeyChain(masterKey, highestDepthToKeep+1);
				
				Key key = new Key();
				key.child = 0;
				key.depth = highestDepthToKeep+1;
	//			key.privateKey = masterKeyToExpose.serializePrivB58();
				key.privateKey = masterKeyToExpose.serializePrivB58(Constants.getNetwork());
				
				keysToBeExposedNew.add(key);
				
				for(Key k : keysToBeExposed) {
					if(k.depth > highestDepthToKeep) {
						keysToBeExposedNew.add(k);
					}
				}
				return keysToBeExposedNew;
			}		
			
			performanceLogger.measure("getKeysOfUsToBeExposed");

			return keysToBeExposed;
		} finally {
			if(set != null) set.close();
			if(stmt != null) stmt.close();
		}
	}
	
	public static void checkKeysFromOtherSide(Connection conn, Channel channel, ArrayList<Key> newKeys) throws Exception {
		if(newKeys == null)
			throw new Exception("keyList is null");
		if(newKeys.size() == 0)
			throw new Exception("keyList is empty");
		PreparedStatement stmt = null;
		ResultSet set = null;
		try {
			/**
			 * Check each key, whether it is corrupted.
			 */
			for(Key k : newKeys) {
				if(!k.check())
					throw new Exception("Private Key does not match with pubkey..");
			}
			int oldMasterDepth = channel.getMasterChainDepth();
			if(newKeys.get(0).child == 0) {
				/**
				 * A new masterkey has been provided..
				 */
				Key masterKey = newKeys.get(0);
				channel.newMasterKey(masterKey);
				newKeys.remove(0);

			

                /**
                 * Remove unneeded keys
                 */
				ArrayList<Key> exposedKeys = MySQLConnection.getKeysExposed(conn, channel, false, channel.getMasterChainDepth());		

				if(exposedKeys != null) {
					
					ArrayList<Key> keysToDelete = new ArrayList<Key>();
					
					DeterministicHierarchy hierachy;
					
					if(SideConstants.RUNS_ON_SERVER) {
						hierachy = channel.getHierachyClient();
					} else {
						hierachy = channel.getHierachyServer();
					}

					for(Key k : exposedKeys) {
						if(k.privateKey != null) {
							if(k.depth >= channel.getMasterChainDepth()) {
								
								List<ChildNumber> list = KeyDerivation.getChildList(k.depth-channel.getMasterChainDepth());
								ChildNumber number = new ChildNumber(k.child, true);
								list.add(number);
								
								DeterministicKey keyDerived = hierachy.get(list, true, true);
								
								if(k.privateKey.equals(Tools.byteToString(keyDerived.getPrivKeyBytes()))) {
									keysToDelete.add(k);
								} else {
                                    System.out.println("Key does not match?");
                                    System.out.println(k.privateKey);
                                    System.out.println(Tools.byteToString(keyDerived.getPrivKeyBytes()));
                                    System.out.println(keyDerived);
									/**
									 * A key that was provided does not matched the path..
									 * I don't know if this could happen accidently, but we should 
									 * do something about this, as we really want clients providing us with
									 * proper data.
									 * 
									 * In extreme cases, terminate the channel here..
									 */
								}
							}
						}
					}
					System.out.println("Delete "+keysToDelete.size()+" keys because we know the masterkey.");
					
					MySQLConnection.deleteKey(conn, channel, keysToDelete);

				}
			
			}
//			performance.measure("checkKeysFromOtherSide 401");

			conn.commit();
			
//			performance.measure("checkKeysFromOtherSide 402");

			
			MySQLConnection.updateKey(conn, channel, newKeys, true, true);
			
//			performance.measure("checkKeysFromOtherSide 403");
			
			stmt = conn.prepareStatement("UPDATE storedkeys SET current_channel=0 WHERE current_channel=1 AND channel_id=?");
			stmt.setInt(1, channel.getId());
			stmt.execute();
			stmt.close();
			
			conn.commit();
//			performance.measure("checkKeysFromOtherSide 41");

			
			ArrayList<String> currentKeys = ScriptTools.getPubKeysOfChannel(channel, !SideConstants.RUNS_ON_SERVER, true);
			stmt = conn.prepareStatement("UPDATE storedkeys SET current_channel=1 WHERE channel_id=? AND pub_key=?");
			for(String s : currentKeys) {
				stmt.setInt(1, channel.getId());
				stmt.setString(2, s);
				stmt.addBatch();
			}
			stmt.executeBatch();
			conn.commit();
			stmt.close();
			
//			performance.measure("checkKeysFromOtherSide 42");

			
			stmt = conn.prepareStatement("SELECT id FROM storedkeys WHERE channel_id=? AND owner=0 AND used=1 AND exposed=0 AND current_channel=0");
			stmt.setInt(1, channel.getId());

			set = stmt.executeQuery();
			if(set.first()) {
				throw new Exception("A key was not provided..");
			}
			
//			performance.measure("checkKeysFromOtherSide 5");

			

			
			
//			/**
//			 * Check if all keys that are marked as used in our database
//			 * 		are exposed, except those used in the very current channel.
//			 * 
//			 * If the current channel tx is 0, this is the first payment.
//			 */
//			boolean notFound = false;
//			performance.measure("checkKeysFromOtherSide 41");
//
//			if(channel.getChannelTxServerID() != 0) {
//				ArrayList<Key> keysToBeExposed = MySQLConnection.getKeysOfOtherSideToBeExposed(conn, channel);
//				performance.measure("checkKeysFromOtherSide 42");
//
//				System.out.println(keysToBeExposed.size()+" should be exposed..");
//				
//				ArrayList<String> a = new ArrayList<String>();
//				ArrayList<String> b = new ArrayList<String>();
//				
//				for(Key k : keysToBeExposed) {
//					a.add(k.publicKey);
//				}
//				for(Key k : newKeys) {
//					b.add(k.publicKey);
//				}
//				
//				performance.measure("checkKeysFromOtherSide 43");
//
//				if(!b.containsAll(a)) {
//					System.out.println("NOT FOUND: ");
//					notFound = true;
//				}
//				performance.measure("checkKeysFromOtherSide 44");
//
//				
////				for(Key key : keysToBeExposed) {
////					boolean found = false;
////					for(Key keyb : newKeys) {
////						if(key.publicKey.equals(keyb.publicKey)) {
////							found = true;
////							break;
////						}
////					}
////					if(!found) {
////						System.out.println("NOT FOUND: "+key.publicKey);
////						notFound = true;
////						break;
////					}
////				}
////				performance.measure("checkKeysFromOtherSide 45");
//
//			}		
//			performance.measure("checkKeysFromOtherSide 5");
//
//			
//			System.out.println("Added "+newKeys.size()+" exposed keys.");
//			
//			
//			if(notFound)
//				throw new Exception("A key, that should have be exposed, was not supplied..");
			

			
			
		} finally {
//			performance.measure("checkKeysFromOtherSide 6");
//			MySQLConnection.updateKey(conn, channel, newKeys, true, true);
//			performance.measure("checkKeysFromOtherSide 7");

			if(set != null) set.close();
			if(stmt != null) stmt.close();
		}
	}
	
	public static ArrayList<Key> getKeysOfOtherSideToBeExposed(Connection conn, Channel channel) throws Exception  {
		PerformanceLogger performance = new PerformanceLogger();

		PreparedStatement stmt = null;
		ResultSet set = null;
		try {
			String sql;
			sql = "SELECT * FROM storedkeys WHERE (channel_id=? AND used=1 AND priv_key IS NULL)";
		
			stmt = conn.prepareStatement(sql);
			stmt.setInt(1, channel.getId());

			set = stmt.executeQuery();
			performance.measure("getKeysOfOtherSideToBeExposed 1");
			
			if(!set.first())
				return null;
			ArrayList<Key> keysToBeExposed = new ArrayList<Key>();
			ArrayList<String> currentKeys = ScriptTools.getPubKeysOfChannel(channel, !SideConstants.RUNS_ON_SERVER, true);
			
			performance.measure("getKeysOfOtherSideToBeExposed 2");

			while(!set.isAfterLast()) {
				
				String pub = set.getString("pub_key");
				boolean expose = true;
				for(String s : currentKeys) {
					if(pub.equals(s)) {
						expose = false;
						break;
					}
				}
				
				Key key = new Key();
				key.child = set.getInt("key_chain_child");
				key.depth = set.getInt("key_chain_depth");
				key.publicKey = set.getString("pub_key");
				key.privateKey = set.getString("priv_key");
				
				if(expose) {
					keysToBeExposed.add(key);
				}
	
				set.next();
			}
			performance.measure("getKeysOfOtherSideToBeExposed 3");

	
			return keysToBeExposed;
		} finally {
			if(set != null) set.close();
			if(stmt != null) stmt.close();
		}
	}
	
	public static ArrayList<Key> getKeys(Connection conn, Channel channel, boolean ourKeys) throws Exception  {
		PreparedStatement stmt = null;
		ResultSet set = null;
		try {
			String sql;
			sql = "SELECT * FROM storedkeys WHERE (channel_id=? AND owner=?)";
			
			stmt = conn.prepareStatement(sql);
			stmt.setInt(1, channel.getId());
			stmt.setInt(2, Tools.boolToInt(ourKeys));
			set = stmt.executeQuery();
			if(!set.first())
				return null;
			
			ArrayList<Key> keys = new ArrayList<Key>();
			
			while(!set.isAfterLast()) {
				Key key = new Key();
				key.child = set.getInt("key_chain_child");
				key.depth = set.getInt("key_chain_depth");
				key.publicKey = set.getString("pub_key");
				key.privateKey = set.getString("priv_key");
				keys.add(key);
				set.next();
			}
			return keys;
		} finally {
			if(set != null) set.close();
			if(stmt != null) stmt.close();
		}
	}
	
	public static ArrayList<Key> getKeysExposed(Connection conn, Channel channel, boolean ourKeys) throws Exception  {
		PreparedStatement stmt = null;
		ResultSet set = null;
		try {
			String sql;
			
	
			sql = "SELECT * FROM storedkeys WHERE (channel_id=? AND exposed=1 AND owner=?)";
			
			stmt = conn.prepareStatement(sql);
			stmt.setInt(1, channel.getId());
			stmt.setInt(2, Tools.boolToInt(ourKeys));
			
			set = stmt.executeQuery();
			
			if(!set.first())
				return null;
			
			ArrayList<Key> keys = new ArrayList<Key>();
			
			while(!set.isAfterLast()) {
	
				Key key = new Key();
				key.child = set.getInt("key_chain_child");
				key.depth = set.getInt("key_chain_depth");
				key.publicKey = set.getString("pub_key");
				key.privateKey = set.getString("priv_key");
				
				
				keys.add(key);
				
	
				set.next();
			}
	
			return keys;
		} finally {
			if(set != null) set.close();
			if(stmt != null) stmt.close();
		}
	}
	
	public static ArrayList<Key> getKeysExposed(Connection conn, Channel channel, boolean ourKeys, int minDepth) throws Exception  {
		if(minDepth == 0) return null;
		PreparedStatement stmt = null;
		ResultSet set = null;
		try {
			String sql;
			
	
			sql = "SELECT * FROM storedkeys WHERE (channel_id=? AND exposed=1 AND owner=? AND key_chain_depth>=?)";
			
			stmt = conn.prepareStatement(sql);
			stmt.setInt(1, channel.getId());
			stmt.setInt(2, Tools.boolToInt(ourKeys));
			stmt.setInt(3, minDepth-1);
			
			set = stmt.executeQuery();
			
			if(!set.first())
				return null;
			
			ArrayList<Key> keys = new ArrayList<Key>();
			
			while(!set.isAfterLast()) {
	
				Key key = new Key();
				key.child = set.getInt("key_chain_child");
				key.depth = set.getInt("key_chain_depth");
				key.publicKey = set.getString("pub_key");
				key.privateKey = set.getString("priv_key");

				keys.add(key);
				
	
				set.next();
			}

			return keys;
		} finally {
			if(set != null) set.close();
			if(stmt != null) stmt.close();
		}
	}
	
	public static void deleteKey(Connection conn, Channel channel, Key key) throws Exception  {
		PreparedStatement stmt = null;
		try {
			String sql = "DELETE FROM storedkeys WHERE (channel_id=? AND pub_key=?)";
		
			stmt = conn.prepareStatement(sql);
			stmt.setInt(1, channel.getId());
			stmt.setString(2, key.publicKey);
			
			stmt.execute();
		} finally {
			if(stmt != null) stmt.close();
		}
	}
	
	public static void deleteKey(Connection conn, Channel channel, ArrayList<Key> keyList) throws Exception  {
		PreparedStatement stmt = null;
		try {
			String sql = "DELETE FROM storedkeys WHERE (channel_id=? AND pub_key=?)";
			
			stmt = conn.prepareStatement(sql);
			
			for(Key key : keyList) {
		
				stmt = conn.prepareStatement(sql);
				stmt.setInt(1, channel.getId());
				stmt.setString(2, key.publicKey);
				
				stmt.addBatch();
			}
			stmt.executeBatch();
		} finally {
			if(stmt != null) stmt.close();
			conn.commit();
		}
	}
	
	public static void deleteUnusedAndExposedKeysFromUs(Connection conn, Channel channel) throws Exception  {
		PreparedStatement stmt = null;
		try {
			String sql = "DELETE FROM storedkeys WHERE (channel_id=? AND (exposed=1 OR used=0) AND owner=1)";
		
			stmt = conn.prepareStatement(sql);
			stmt.setInt(1, channel.getId());

			stmt.execute();
			conn.commit();
		} finally {
			if(stmt != null) stmt.close();
		}
	}
	
	public static void deleteUnusedKeyFromOtherSide(Connection conn, Channel channel) throws Exception  {
		PreparedStatement stmt = null;
		try {
			String sql = "DELETE FROM storedkeys WHERE (channel_id=? AND exposed=0 AND used=0 AND owner=0)";
		
			stmt = conn.prepareStatement(sql);
			stmt.setInt(1, channel.getId());

			stmt.execute();
			conn.commit();
		} finally {
			if(stmt != null) stmt.close();
		}
	}
	
	public static ArrayList<Key> getPubKeysOfChannel(Connection conn, Channel channel, boolean serverSide, boolean temporary) throws ScriptException, SQLException {
		ArrayList<Key> currentKeys = new ArrayList<Key>();
		
		ArrayList<String> pubKeys = ScriptTools.getPubKeysOfChannel(channel, serverSide, temporary);
		
		return currentKeys;
	}
	
	public static int checkChannelForReceiving(Connection conn, String receiver) throws Exception {
		PreparedStatement stmt = null;
		ResultSet set = null;
		try {
			String address = "";
			boolean isAddress = false;
			try {
				address = new Address(Constants.getNetwork(), receiver).toString();
				isAddress = true;
			} catch(Exception e) {
				address = receiver;
			}
			
			String sql = "";
			
			if(receiver.length() == 10) {
				address = receiver+"%";
				sql = "SELECT id FROM channels WHERE pub_key_client LIKE ? AND is_ready=1";
			} else {
			
				if(isAddress) {
					sql = "SELECT id FROM channels WHERE change_address_client=? AND is_ready=1";
				} else {
					sql = "SELECT id FROM channels WHERE pub_key_client=? AND is_ready=1";
				}
			}
			
			stmt = conn.prepareStatement(sql);
						
			stmt.setString(1, address);
			set = stmt.executeQuery();
			
			if(set.first()) {
				return set.getInt("id");
			} else
				return 0;
		} finally {
			if(set != null) set.close();
			if(stmt != null) stmt.close();
		}
			
		
	}
	
	/**
	 *  Create a new channel based on the request MessageOneRequest
	 *  Will also create a new master key for us to use in this channel
	 * 
	 * @param conn
	 * @param message
	 * @param pubkeyChannel
	 * @return
	 * @throws SQLException
	 */
	public static Channel createNewChannel(Connection conn, String changeAddressClient) throws SQLException {
		PreparedStatement stmt = null;
		try {
	
			stmt = conn.prepareStatement("INSERT INTO channels (pub_key_client, change_address_client, master_priv_key_client, establish_phase, is_ready, key_chain_depth) values (?, ?, ?, ?, ?, ?)");
			int count = MySQLConnection.getKeyCount(conn);
			if(!SideConstants.RUNS_ON_SERVER)
				count = (int) (Math.random() * 10000000);
			DeterministicKey key = KeyDerivation.getMasterKey(count);
			
			String masterKey = key.serializePrivB58(Constants.getNetwork());
	//		String masterKey = key.serializePrivB58();
			
			stmt.setString(1, Tools.byteToString(key.getPubKey()));
			stmt.setString(2, changeAddressClient);
			stmt.setString(3, masterKey);
			stmt.setInt(4, 1);
            stmt.setInt(5, 0);
            /**
             * Workaround for now. Also limits the maximum key iterations to 9999
             */
            stmt.setInt(6, 9999999);

			
		    stmt.execute();
		    
		    stmt.close();
		    
		    conn.commit();
		    
		    Channel channel = MySQLConnection.getChannel(conn, Tools.byteToString(key.getPubKey()));

//		    channel.setMasterPrivateKeyClient(masterKey);
//		    channel.setPubKeyClient(Tools.byteToString(key.getPubKey()));
//		    channel.setChangeAddressClient(changeAddressClient);

			System.out.println("New channel: "+Tools.byteToString58(key.getPubKey()));
			
			return channel;
		} finally {
			if(stmt != null) stmt.close();
		}
	}
	
	/**
	 * Delete all specific Outputs
	 * 
	 * @param conn
	 * @param output
	 * @throws SQLException
	 */
	public static void deleteAllOutputs(Connection conn) throws SQLException {
		PreparedStatement stmt = null;
		try {		
			stmt = conn.prepareStatement("DELETE FROM outputs");
			stmt.execute();
		} finally {
			if(stmt != null) stmt.close();
		}
	}
	
	/**
	 * Delete up one specific Output
	 * 
	 * @param conn
	 * @param output
	 * @throws SQLException
	 */
	public static void deleteOutput(Connection conn, Output output) throws SQLException {
		PreparedStatement stmt = null;
		try {	
	
			stmt = conn.prepareStatement("DELETE FROM outputs WHERE transaction_hash=?");
			stmt.setString(1, output.getHash());
			stmt.execute();
		} finally {
			if(stmt != null) stmt.close();
		}
	}
	
	public static void deleteOutputByChannel(Connection conn, Channel channel) throws SQLException {
		PreparedStatement stmt = null;
		try {	
	
			stmt = conn.prepareStatement("DELETE FROM outputs WHERE channel_id=?");
			stmt.setInt(1, channel.getId());
			stmt.execute();
		} finally {
			if(stmt != null) stmt.close();
		}
	}
	
	/**
	 * Return a channel.
	 * 
	 * @param conn
	 * @param pubKey
	 * @return
	 * @throws SQLException
	 */
	public static Channel getChannel(Connection conn, String pubKey) throws SQLException {
		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement("SELECT * FROM channels WHERE pub_key_client=? FOR UPDATE");
			stmt.setString(1, pubKey);
					
			ResultSet result = stmt.executeQuery();
			result.first();
			Channel c = new Channel(result);
			c.conn = conn;
			result.close();
			return c;
		} finally {
			if(stmt != null) stmt.close();
		}
	}
	
	public static Channel getChannel(Connection conn, int id) throws SQLException {
		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement("SELECT * FROM channels WHERE id=? FOR UPDATE");
			stmt.setInt(1, id);
					
			ResultSet result = stmt.executeQuery();
			result.first();
			Channel c = new Channel(result);
			c.conn = conn;
			result.close();
			return c;
		} finally {
			if(stmt != null) stmt.close();
		}
	}
	
	public static boolean testIfChannelExists(Connection conn, String pubKey) throws SQLException {
		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement("SELECT pub_key_client FROM channels WHERE pub_key_client=?");		
			stmt.setString(1, pubKey);
					
			ResultSet result = stmt.executeQuery();
			boolean a = result.first();
			result.close();
			return a;
		} finally {
			if(stmt != null) stmt.close();
		}
	}
	
	public static ArrayList<Channel> getChannelTransactions(Connection conn) throws SQLException {
		Statement stmt = null;
		ResultSet set = null;
		try {
			ArrayList<Transaction> transactionList = new ArrayList<Transaction>();
			ArrayList<Channel> list = new ArrayList<Channel>();
	
			stmt = conn.createStatement();
			String query = "SELECT * FROM channels WHERE is_ready=1";
			ResultSet results = stmt.executeQuery(query);
			
			
			if(results.first()) {
				while(!results.isAfterLast()) {
					list.add(new Channel(results));
					results.next();
				}
			}
			
			for(Channel channel : list) {
				channel.conn = conn;
				transactionList.add(channel.getChannelTxServer());
			}
			
			return list;
		} finally {
			if(set != null) set.close();
			if(stmt != null) stmt.close();
		}
	}
	
	public static Connection getInstance() throws SQLException {
		// if (conn == null)
		// new MySQLConnection();
		// return conn;
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Connection conn =  DriverManager.getConnection(SideConstants.DATABASE_CONNECTION);
		
		return conn;

	}
	
	public static Connection getInstance(int version) throws SQLException {
		// if (conn == null)
		// new MySQLConnection();
		// return conn;
		try {
			
			Class.forName("org.h2.Driver").newInstance();
			Connection conn = null;
            conn = DriverManager.getConnection(SideConstants.getDatabaseConnection(version));
			return conn;


		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return null;

	}

	public static HashMap<Integer, Channel> getActiveChannels(Connection conn) throws SQLException {
		PreparedStatement stmt = null;
		HashMap<Integer, Channel> channelList = new HashMap<>();
		try {
			stmt = conn.prepareStatement("SELECT * FROM channels WHERE is_ready=1");	

			ResultSet result = stmt.executeQuery();
			
			if(!result.first())
				return channelList;
			
			while(!result.isAfterLast()) {
				Channel c = new Channel(result);
				c.conn = conn;
				channelList.put(c.getId(), c);
				result.next();
			}
			result.close();
			return channelList;
		} finally {
			if(stmt != null) stmt.close();
		}
	}

	public static ArrayList<Payment> getPaymentsOpen(Connection conn, int channelId) throws SQLException {
		PreparedStatement stmt = null;
		try {
			ArrayList<Payment> list = new ArrayList<Payment>();
			stmt = conn.prepareStatement("SELECT * FROM payments WHERE ( channel_id_receiver=? AND include_in_receiver_channel=0 AND phase=0 ) ORDER BY id DESC");
			stmt.setInt(1, channelId);
			ResultSet results = stmt.executeQuery();
			
			if(results.first()) {
				while(!results.isAfterLast() ) {
					Payment payment = new Payment(results);
					if(payment.getChannelIdReceiver() == channelId) {
						payment.paymentToServer = false;
					} else {
						payment.paymentToServer = true;
					}
					
					list.add(payment);
					results.next();
				}
			}
			results.close();
			return list;
		} finally {
			if(stmt != null) stmt.close();
		}		
	}

	public static ArrayList<Payment> getPaymentsSettled(Connection conn, int channelId) throws SQLException {
		PreparedStatement stmt = null;
		try {
			ArrayList<Payment> list = new ArrayList<Payment>();
			stmt = conn.prepareStatement("SELECT * FROM payments WHERE ( ( (channel_id_sender=? AND include_in_sender_channel=0 AND phase=1) OR (channel_id_receiver=? AND include_in_receiver_channel=0 AND phase=2) ) ) ORDER BY id DESC");
			stmt.setInt(1, channelId);
			stmt.setInt(2, channelId);
			ResultSet results = stmt.executeQuery();

			if(results.first()) {
				while(!results.isAfterLast() ) {
					Payment payment = new Payment(results);
					if(payment.getChannelIdReceiver() == channelId) {
						payment.paymentToServer = false;
					} else {
						payment.paymentToServer = true;
					}
					
					list.add(payment);
					results.next();
				}
			}
			results.close();
			return list;
		} finally {
			if(stmt != null) stmt.close();
		}		
	}
	
	public static ArrayList<Payment> getPaymentsRefunded(Connection conn, int channelId) throws SQLException {
		PreparedStatement stmt = null;
		try {
			ArrayList<Payment> list = new ArrayList<Payment>();
			stmt = conn.prepareStatement("SELECT * FROM payments WHERE ( ( (channel_id_sender=? AND include_in_sender_channel=0 AND phase=6) OR (channel_id_receiver=? AND include_in_receiver_channel=0 AND phase=6) ) ) ORDER BY id DESC");
			stmt.setInt(1, channelId);
			stmt.setInt(2, channelId);
			ResultSet results = stmt.executeQuery();

			if(results.first()) {
				while(!results.isAfterLast() ) {
					Payment payment = new Payment(results);
					if(payment.getChannelIdReceiver() == channelId) {
						payment.paymentToServer = false;
					} else {
						payment.paymentToServer = true;
					}
					
					list.add(payment);
					results.next();
				}
			}
			results.close();
			return list;
		} finally {
			if(stmt != null) stmt.close();
		}		
	}
	
	
	/**
	 * Get a tamper-proof key count for generating the master private key for a channel.
	 * Ensures that each channel will have a unique key. 
	 * 
	 * @param conn 
	 * @return Use this hardened key for the channel.
	 * @throws SQLException
	 */
	public static int getKeyCount(Connection conn) throws SQLException {
		PreparedStatement stmt = null;
		try {
	
			Statement query;
			query = conn.createStatement();
						
			
			ResultSet result = query.executeQuery("SELECT key_count FROM constants FOR UPDATE;");
			result.first();
			int id = result.getInt(1);
			id++;
			
			query.execute("UPDATE constants SET key_count=" + id);
			
			query.close();
			conn.commit();
	
			return id;
		} finally {
			if(stmt != null) stmt.close();
		}		
		
	}
	
	public static ArrayList<Key> createKeys(Connection conn, Channel channel, int amount) throws SQLException {
		Statement stmt = null;
		ResultSet set = null;
		try {
	
	
			ArrayList<Key> keyList = new ArrayList<Key>();
			
			Statement query = conn.createStatement();
			set = query.executeQuery("SELECT timestamp_close, key_chain_depth, key_chain_child, master_priv_key_server, master_priv_key_client FROM channels WHERE id='"+channel.getId()+ "' FOR UPDATE");
//			set = query.executeQuery("SELECT timestamp_close, key_chain_depth, key_chain_child, master_priv_key_server, master_priv_key_client FROM channels WHERE pub_key_client='"+channel.getId()+"'");
			set.first();
			
			int depth = set.getInt("key_chain_depth");
			int child = set.getInt("key_chain_child");
			int close = set.getInt("timestamp_close");
			
			int diff = ( (int) Math.floor((close-Tools.currentTime()) / Constants.TIMEFRAME_PER_KEY_DEPTH ) + 1);
			
			if(diff<depth) {
				depth = diff;
				child = 1;
			}
			
	
			
			String masterKey;
			if(SideConstants.RUNS_ON_SERVER) {
				masterKey = set.getString("master_priv_key_server");
			} else {
				masterKey = set.getString("master_priv_key_client");
			}
			
			DeterministicKey root = KeyDerivation.calculateKeyChain(masterKey, depth);
			DeterministicHierarchy hierachy = new DeterministicHierarchy(root);
			DeterministicKey key;

			for(int i=0; i<amount; i++ ) {
				ArrayList<ChildNumber> list = new ArrayList<ChildNumber>();
				ChildNumber childNumber = new ChildNumber(child+i+1, true);
				list.add(childNumber);
				
				key = hierachy.get(list, true, true);
				
				Key k = new Key();
				k.child = child+i+1;
				k.depth = depth;
				k.privateKey = Tools.byteToString(key.getPrivKeyBytes());
				k.publicKey = Tools.byteToString(key.getPubKey());
				
				
				
				
				keyList.add(k);
			}
	
//			System.out.println("UPDATE channels SET key_chain_depth="+depth+", key_chain_child="+(child+amount)+" WHERE id='"+channel.getId()+ "'");
			query.execute("UPDATE channels SET key_chain_depth="+depth+", key_chain_child="+(child+amount)+" WHERE id='"+channel.getId()+ "'");
			conn.commit();
			
			MySQLConnection.addKey(conn, keyList, channel.getId(), true);
			conn.commit();
			
			channel.setKeyChainChild(child+amount);
			channel.setKeyChainDepth(depth);
			
			
			return keyList;
	
		} finally {
			if(set != null) set.close();
			if(stmt != null) stmt.close();
		}
	}
	
	public static int getCurrentPaymentsAmount(Connection conn, Channel channel)  throws SQLException {
		PreparedStatement stmt = null;
		ResultSet set = null;
		try {
			stmt = conn.prepareStatement("SELECT count(*) FROM payments WHERE (channel_id_sender=? AND include_in_sender_channel=1) OR (channel_id_receiver=? AND include_in_receiver_channel=1)");
			stmt.setInt(1, channel.getId());
			stmt.setInt(2, channel.getId());

			set = stmt.executeQuery();
			set.first();
	
			return set.getInt(1);
	
		} finally {
			if(set != null) set.close();
			if(stmt != null) stmt.close();
		}
	}
	
	public static int getPaymentsAmount(Connection conn, Channel channel, int phase)  throws SQLException {
		PreparedStatement stmt = null;
		ResultSet set = null;
		try {
			stmt = conn.prepareStatement("SELECT count(*) FROM payments WHERE ( ( channel_id_sender=? OR channel_id_receiver=? ) AND phase=?)");
			stmt.setInt(1, channel.getId());
			stmt.setInt(2, channel.getId());
			stmt.setInt(3, phase);
	
			set = stmt.executeQuery();
			set.first();
	
			return set.getInt(1);
	
		} finally {
			if(set != null) set.close();
			if(stmt != null) stmt.close();
		}
	}
	
	public static ArrayList<Channel> getOpeningTransactions(Connection conn) throws SQLException {
		Statement stmt = null;
		ResultSet set = null;
		try {
			ArrayList<Transaction> transactionList = new ArrayList<Transaction>();
			ArrayList<Channel> list = new ArrayList<Channel>();
	
			stmt = conn.createStatement();
			String query = "SELECT * FROM channels WHERE establish_phase=4";
			set = stmt.executeQuery(query);
			
			
			if(set.first()) {
				while(!set.isAfterLast()) {
					list.add(new Channel(set));
					set.next();
				}
			}
			
			for(Channel channel : list) {
				channel.conn = conn;
				transactionList.add(channel.getOpeningTx());
			}
			
			return list;
		} finally {
			if(set != null) set.close();
			if(stmt != null) stmt.close();
		}
	}
	
	
	
	/**
	 * Get all available outputs for channel creation.
	 * Lock up each output that will get used in a channel
	 * 
	 * @param conn
	 * @throws SQLException
	 * @throws AddressFormatException 
	 */
	public static Transaction getOutAndInputsForChannel(Connection conn, ArrayList<Output> list, int channelId, long myShare, Transaction transactionOld, Address changeAddress, boolean sign, boolean withFees) throws SQLException, AddressFormatException {
		PreparedStatement stmt = null;
		ResultSet set = null;
		try {
	
			int amountForeignInputs = transactionOld.getInputs().size();

//			if(SideConstants.RUNS_ON_SERVER) {
//				stmt = conn.prepareStatement("SELECT * FROM outputs WHERE timestamp_locked<? OR timestamp_locked=0 ORDER BY value ASC");
//				stmt.setInt(1, (Tools.currentTime()+Constants.MAX_CHANNEL_CREATION_TIME) );
//				set = stmt.executeQuery();
//				set.first();
//
//
//				while(!set.isAfterLast()) {
//					Output output = new Output(set);
//					output.setChannelId(channelId);
//
//					list.add(output);
//					set.next();
//				}
//			} else {
//				list = ThunderContext.instance.outputList;
//			}
			
			Transaction transaction = null; 
	
			
			/**
			 * Lets see if we can pay the payment with only one of our outputs..
			 */
			
			long value;
			if(withFees) {
				value = (myShare + Tools.getTransactionFees(transactionOld.getInputs().size()+1, transactionOld.getOutputs().size() + 1));
			} else {
				value = myShare;
			}

            /**
             * TODO: I changed >= to >, such that there will always be a change.
             *          In the future it would be nice to pay exact amounts whenever possible.
             */
			for(Output o : list) {
				if(o.getValue() > value && transaction == null) {
					/**
					 * Ok, found a suitable output, need to split the change
					 */
					transaction  = transactionOld;
	
					if(withFees) {
						transaction.addOutput(Coin.valueOf( o.getValue() - myShare - Tools.getTransactionFees(transactionOld.getInputs().size()+1, transactionOld.getOutputs().size() + 1)), changeAddress);
					} else {
						transaction.addOutput(Coin.valueOf(o.getValue() - myShare), changeAddress);
					}
	
					lockOutputs(conn, o);
					if(sign) {
						
						transaction.addInput(new Sha256Hash(o.getHash()), o.getVout(), o.getTransactionOutput().getScriptPubKey());
						
						Sha256Hash hash = transaction.hashForSignature(amountForeignInputs, o.getTransactionOutput().getScriptBytes(), SigHash.ALL, false);
						ECDSASignature signature = o.getECKey().sign(hash);
						
						TransactionSignature sig = new TransactionSignature(signature, SigHash.ALL, false);
						
						Script script = ScriptBuilder.createInputScript(sig, o.getECKey());
						transaction.getInput(amountForeignInputs).setScriptSig(script);
						
	//					transaction.addSignedInput(o.getTransactionOutput(), o.getECKey());
					} else {
						transaction.addInput(new Sha256Hash(o.getHash()), o.getVout(), new Script( o.getTransactionOutput().getScriptBytes() ));
					}
				}
			}
			if(transaction == null) {
			/**
			 * None of our outputs alone is sufficient, have to add multiples..
			 */
				ArrayList<Output> inputList = new ArrayList<Output>();
				long totalValue = 0;
				for(Output o : list) {
					if(totalValue >= myShare) continue;
					totalValue += o.getValue();
					inputList.add(o);
				}
				if(totalValue < myShare) {
					/**
					 * Not enough outputs in total to pay for the channel..
					 */
				} else {
					transaction  = transactionOld;
					
					if(withFees) {
						transaction.addOutput(Coin.valueOf( totalValue - myShare - Tools.getTransactionFees(transactionOld.getInputs().size() + inputList.size(), transactionOld.getOutputs().size() + 1)), changeAddress);
					} else {
						transaction.addOutput(Coin.valueOf(totalValue - myShare), changeAddress);
					}
					
					/**
					 * Add all inputs first before starting to sign
					 */
					for(Output o : inputList) {
						lockOutputs(conn, o);
						transaction.addInput(new Sha256Hash(o.getHash()), o.getVout(), new Script( o.getTransactionOutput().getScriptBytes() ));

					}
					
					/**
					 * Sign all of our inputs..
					 */	
					if(sign) {
						int j=0;
						for(int i=amountForeignInputs; i<transaction.getInputs().size(); ++i) {
							Output o = inputList.get(j);
							
							Sha256Hash hash = transaction.hashForSignature(i, o.getTransactionOutput().getScriptBytes(), SigHash.ALL, false);
							ECDSASignature signature = o.getECKey().sign(hash);
							
							TransactionSignature sig = new TransactionSignature(signature, SigHash.ALL, false);
							
							Script script = ScriptBuilder.createInputScript(sig, o.getECKey());
							transaction.getInput(i).setScriptSig(script);
							j++;
						}
					}
	
				}
			}

			
			conn.commit();
			return transaction;
	
		} finally {
			if(set != null) set.close();
			if(stmt != null) stmt.close();
		}		
	}
	/**
	 * Get one specific output
	 * 
	 * @param conn
	 * @throws SQLException
	 */
	public static Output getOutput(Connection conn, String transactionHash, long vout) throws SQLException {
		PreparedStatement stmt = null;
		ResultSet set = null;
		try {
	
			stmt = conn.prepareStatement("SELECT * FROM outputs WHERE transaction_hash=? AND vout=?");
			stmt.setString(1, transactionHash);
			stmt.setLong(2, vout);
			set = stmt.executeQuery();
			set.first();
			Output output = new Output(set);
			return output;
	
		} finally {
			if(set != null) set.close();
			if(stmt != null) stmt.close();
		}
	}
	
	public static Transaction getTransaction(Connection conn, int id) throws SQLException {
		PreparedStatement stmt = null;
		ResultSet set = null;
		try {
			if(id == 0) return null;

			stmt = conn.prepareStatement("SELECT * FROM transactions WHERE id=?");
			stmt.setInt(1, id);
			set = stmt.executeQuery();
	
			if(set.first()) {
				Transaction transaction = new Transaction(Constants.getNetwork(), set.getBytes("data"));
				return transaction;		
			} 
			return null;
		} finally {
			if(set != null) set.close();
			if(stmt != null) stmt.close();
		}
	}
	
	/**
	 * Get all available outputs for channel creation.
	 * Lock up each output that will get used in a channel
	 * 
	 * @param conn
	 * @throws SQLException
	 */
	public static ArrayList<Output> getUnlockedOutputs(Connection conn) throws SQLException {
		PreparedStatement stmt = null;
		ResultSet set = null;
		try {
	
			stmt = conn.prepareStatement("SELECT * FROM outputs WHERE timestamp_locked<" + (Tools.currentTime() + Constants.MAX_CHANNEL_CREATION_TIME));
			set = stmt.executeQuery();
			set.first();
			
			ArrayList<Output> list = new ArrayList<Output>();
			
			while(!set.isAfterLast()) {
				
				Output output = new Output();
				
				output.setHash(set.getString("transaction_hash"));
				output.setVout(set.getInt("vout"));
				output.setValue(set.getLong("value"));
				output.setPrivateKey(set.getString("private_key"));
				output.setLock(set.getInt("timestamp_locked"));
				output.setTransactionOutput(new TransactionOutput(Constants.getNetwork(), null, set.getBytes("transaction_output"), 0));
				
				list.add(output);
				set.next();
			}
			return list;
	
		} finally {
			if(set != null) set.close();
			if(stmt != null) stmt.close();
		}
	}
	
	public static void insertTransaction(Connection conn, Transaction t) throws SQLException {
		Statement stmt = null;
		ResultSet set = null;
		try {

			stmt = conn.createStatement();
	
			String sql = "INSERT INTO transactions VALUES(NULL, '"+t.getHashAsString()+"', 'test', 1, NULL)";
			stmt.execute(sql);
		} finally {
			if(set != null) set.close();
			if(stmt != null) stmt.close();
		}

	}

    public static void deleteTransaction(Connection conn, int id) throws SQLException {
        if(id != 0) {
            Statement stmt = null;
            try {
                stmt = conn.createStatement();

                String sql = "DELETE FROM transactions WHERE id="+id;
                stmt.execute(sql);

            } finally {
                if(stmt != null) stmt.close();
            }
        }
    }

    public static void deletePayment(Connection conn, int id) throws SQLException {
        if(id != 0) {
            Statement stmt = null;
            try {
                stmt = conn.createStatement();

                String sql = "DELETE FROM payments WHERE id="+id;
                stmt.execute(sql);
                System.out.println("DELETE FROM payments WHERE id="+id);
            } finally {
                if(stmt != null) stmt.close();
            }
        }
    }
	
	/**
	 * Lock up one specific Output
	 * 
	 * @param conn
	 * @param output
	 * @throws SQLException
	 */
	public static void lockOutputs(Connection conn, Output output) throws SQLException {
		PreparedStatement stmt = null;
		ResultSet set = null;
		try {
			stmt = conn.prepareStatement("UPDATE outputs SET timestamp_locked="+(Tools.currentTime()+Constants.MAX_CHANNEL_CREATION_TIME)+", channel_id=? WHERE transaction_hash=?");
			stmt.setInt(1, output.getChannelPubKey());
			stmt.setString(2, output.getHash());
			stmt.execute();
		} finally {
			if(set != null) set.close();
			if(stmt != null) stmt.close();
		}
	}

	/**
	 * Insert a message into the database.
	 * Up to now, insert all messages, sent and received, into the database.
	 * 
	 * 
	 * @param conn
	 * @param message
	 * @throws SQLException
	 */
	public static void saveMessage(Connection conn, Message message) throws SQLException {
		PreparedStatement stmt = null;
		ResultSet set = null;
		try {
	
			stmt = conn.prepareStatement("INSERT INTO messages (id, type, message, timestamp, pubkey, signature) values (?, ?, ?, ?, ?, ?)");
			
			stmt.setString(1, null);
			stmt.setInt(2, message.type);
			stmt.setString(3, message.data);
			stmt.setInt(4, message.timestamp);
			stmt.setString(5, message.pubkey);
			stmt.setString(6, message.signature);
			
		    stmt.executeUpdate();
    
    
		} finally {
			if(set != null) set.close();
			if(stmt != null) stmt.close();
		}
	}
	
	
	
	
	public static void updateChannel(Connection conn, Channel channel) throws SQLException {
		PreparedStatement stmt = null;
		ResultSet set = null;
		try {
	
			
			channel.updateTransactionsToDatabase(conn);
			conn.commit();

//            stmt = conn.prepareStatement("UPDATE channels SET master_priv_key_client=?, master_priv_key_server=?, initial_amount_server=?, initial_amount_client=?, amount_server=?, amount_client=?, timestamp_open=?, timestamp_close=?, opening_tx=?, refund_tx_server=?, refund_tx_client=?, channel_tx_server=?, channel_tx_revoke_server=?, channel_tx_client=?, channel_tx_revoke_client=?, has_open_payments=?, establish_phase=?, is_ready=?, key_chain_depth=?, key_chain_child=?, channel_tx_server_temp=?, channel_tx_client_temp=?, channel_tx_revoke_client_temp=?, channel_tx_revoke_server_temp=?, payment_phase=?, master_chain_depth=?, opening_tx_hash=?, change_address_server=?, pub_key_server=?, pub_key_client=? WHERE id=?");
            stmt = conn.prepareStatement("UPDATE channels SET master_priv_key_client=?, master_priv_key_server=?, initial_amount_server=?, initial_amount_client=?, amount_server=?, amount_client=?, timestamp_open=?, timestamp_close=?, opening_tx=?, refund_tx_server=?, refund_tx_client=?, channel_tx_server=?, channel_tx_revoke_server=?, channel_tx_client=?, channel_tx_revoke_client=?, has_open_payments=?, establish_phase=?, is_ready=?, channel_tx_server_temp=?, channel_tx_client_temp=?, channel_tx_revoke_client_temp=?, channel_tx_revoke_server_temp=?, payment_phase=?, master_chain_depth=?, opening_tx_hash=?, change_address_server=?, pub_key_server=?, pub_key_client=? WHERE id=?");

			stmt.setString(1, channel.getMasterPrivateKeyClient());
			stmt.setString(2, channel.getMasterPrivateKeyServer());
			stmt.setLong(3, channel.getInitialAmountServer());
			stmt.setLong(4, channel.getInitialAmountClient());
			stmt.setLong(5, channel.getAmountServer());
			stmt.setLong(6, channel.getAmountClient());

			stmt.setInt(7, channel.getTimestampOpen());
			stmt.setInt(8, channel.getTimestampClose());

			stmt.setInt(9, channel.getOpeningTxID());
			stmt.setInt(10, channel.getRefundTxServerID());
			stmt.setInt(11, channel.getRefundTxClientID());
			stmt.setInt(12, channel.getChannelTxServerID());
			stmt.setInt(13, channel.getChannelTxRevokeServerID());
			stmt.setInt(14, channel.getChannelTxClientID());
			stmt.setInt(15, channel.getChannelTxRevokeClientID());
			stmt.setInt(16, Tools.boolToInt(channel.getHasOpenPayments()));
			stmt.setInt(17, channel.getEstablishPhase());
			stmt.setInt(18, Tools.boolToInt(channel.isReady()));
/**
 * We wont update those for now. These fields should actually only be changed when creating new keys..
 */
//			stmt.setInt(19, channel.getKeyChainDepth());
//			stmt.setInt(20, channel.getKeyChainChild());

			stmt.setInt(19, channel.getChannelTxServerTempID());
			stmt.setInt(20, channel.getChannelTxClientTempID());
			stmt.setInt(21, channel.getChannelTxRevokeServerTempID());
			stmt.setInt(22, channel.getChannelTxRevokeClientTempID());

			stmt.setInt(23, channel.getPaymentPhase());
			stmt.setInt(24, channel.getMasterChainDepth());

			stmt.setString(25, channel.getOpeningTxHash());

			stmt.setString(26, channel.getChangeAddressServer());

			stmt.setString(27, channel.getPubKeyServer());
			stmt.setString(28, channel.getPubKeyClient());
			stmt.setInt(29, channel.getId());


            stmt.execute();
			conn.commit();

		} finally {
			if(set != null) set.close();
			if(stmt != null) stmt.close();
		}
	}

	
	Connection conn;

	
	private MySQLConnection()  {



	}
	
}
