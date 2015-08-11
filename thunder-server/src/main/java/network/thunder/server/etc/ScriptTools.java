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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import network.thunder.server.database.objects.Channel;
import network.thunder.server.database.objects.KeyWrapper;

import org.bitcoinj.core.ECKey.ECDSASignature;
import org.bitcoinj.core.Transaction.SigHash;
import org.bitcoinj.core.ScriptException;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptChunk;

// TODO: Auto-generated Javadoc
/**
 * The Class ScriptTools.
 */
public class ScriptTools {
	
	/**
	 *  
	 * Payment outputscript has different forms, depending whether it's serverside or clientside and also depending in which direction the payment goes:
	 * 
	 * 
	 * 	Payment client->server
	 * 		serverside:
	 * 			OP_SIZE <20> OP_EQUAL 
	 * 			OP_IF 
	 * 				OP_HASH160 <R> OP_EQUALVERIFY OP_2 <clientPubKey> <serverPubKeyTemp1> OP_2 OP_CHECKMULTISIG
	 * 			OP_ELSE
	 * 				OP_2 <clientPubKey> <serverPubKeyTemp2> OP_2 OP_CHECKMULTISIG
	 * 			OP_END
	 * 
	 * 			getting solved by:
	 * 				H clientSig serverSig1 
	 * 					OR 
	 * 				clientSig serverSig2
	 * 
	 * 		clientside:
	 * 			OP_SIZE <20> OP_EQUAL 
	 * 			OP_IF 
	 * 				OP_HASH160 <R> OP_EQUALVERIFY <serverPubKey> OP_CHECKSIG
	 * 			OP_ELSE
	 * 				OP_2 <clientPubKeyTemp> <serverPubKey> OP_2 OP_CHECKMULTISIG
	 * 			OP_END
	 * 
	 * 			getting solved by:
	 * 				H serverSig
	 * 					OR
	 * 				cliengSig1 serverSig 
	 * 
	 * 	 Payment server->client
	 * 		serverside:
	 * 			OP_SIZE <20> OP_EQUAL 
	 * 			OP_IF 
	 * 				OP_HASH160 <R> OP_EQUALVERIFY OP_2 <clientPubKey> <serverPubKeyTemp1> OP_2 OP_CHECKMULTISIG
	 * 			OP_ELSE
	 * 				OP_2 <clientPubKey> <serverPubKeyTemp2> OP_2 OP_CHECKMULTISIG
	 * 			OP_END
	 * 
	 * 			getting solved by:
	 * 				H clientSig serverSig1 
	 * 					OR 
	 * 				clientSig serverSig2
	 * 
	 * 		clientside:
	 * 			OP_SIZE <20> OP_EQUAL 
	 * 			OP_IF 
	 * 				OP_HASH160 <R> OP_EQUALVERIFY OP_2 <clientPubKeyTemp> <serverPubKey> OP_2 OP_CHECKMULTISIG
	 * 			OP_ELSE
	 * 				<serverPubKey> OP_CHECKSIG
	 * 			OP_END
	 * 
	 * 			getting solved by:
	 * 				H clientSig1 serverSig
	 * 					OR
	 * 				serverSig .
	 *
	 * @param output the output
	 * @param channel the channel
	 * @param keyList the key list
	 * @param serverSide the server side
	 * @return true, if successful
	 * @throws Exception the exception
	 */
	
	public static boolean checkRevokeScript(TransactionOutput output, Channel channel, KeyWrapper keyList, boolean serverSide) throws Exception {
		
		/**
		 * Revoke Script should be a 2-of-2 multisig with a pubkey of client/server and a tempkey of server/client..
		 */
		
		
		boolean scriptIsCorrect = false;
		List<ScriptChunk> scriptChunks = output.getScriptPubKey().getChunks();
		
		if(scriptChunks.size() != 5)
			return false;
		
		int index = 0;
		for(ScriptChunk chunk : scriptChunks) {
			boolean chunkIsCorrect = false;
			boolean opcode = chunk.isOpCode();
			boolean pushData = chunk.isPushData();
						
			if(index == 0) {
				if(opcode) {
					//OP_2
					if(chunk.equalsOpCode(82))
						chunkIsCorrect = true;
				}
			} else if(index == 1) {
				if(pushData) {
					if(serverSide) {
						//OP_PUSHDATA clientPubKey
						if(chunk.data.length == 33 && chunk.opcode == 33) {
							String a = Tools.byteToString(chunk.data);
						
							if(Tools.byteToString(chunk.data).equals(channel.getPubKeyClient()))
								chunkIsCorrect = true;
						}
					} else {
						//OP_PUSHDATA serverPubKeyTemp
						if(chunk.data.length == 33 && chunk.opcode == 33)
							if(keyList.checkKey(Tools.byteToString(chunk.data)))
								chunkIsCorrect = true;
					}
				}
			} else if(index == 2) {
				if(pushData) {
					if(serverSide) {
						//OP_PUSHDATA clientPubKeyTemp
						if(chunk.data.length == 33 && chunk.opcode == 33)
							if(keyList.checkKey(Tools.byteToString(chunk.data)))
								chunkIsCorrect = true;
					} else {
						//OP_PUSHDATA serverPubKey
						if(chunk.data.length == 33 && chunk.opcode == 33) {
							String a = Tools.byteToString(chunk.data);
							String b = channel.getPubKeyServer();
							if(Tools.byteToString(chunk.data).equals(channel.getPubKeyServer()))
								chunkIsCorrect = true;
						}
					}

				}
			} else if(index == 3) {
				if(opcode) {
					//OP_2
					if(chunk.equalsOpCode(82))
						chunkIsCorrect = true;
				}
			} else if(index == 4) {
				if(opcode) {
					//OP_CHECKMULTISIG
					if(chunk.equalsOpCode(174))
						chunkIsCorrect = true;
				}
			}			
			
			if(!chunkIsCorrect) {
				System.out.println("Error in chunk "+index);
				break;
			}
			if(index == 4)
				scriptIsCorrect = true;
			index++;
		}
		return scriptIsCorrect;
	}
	
	/**
	 * Gets the revoke script.
	 *
	 * @param keyList the key list
	 * @param channel the channel
	 * @param serverSide the server side
	 * @return the revoke script
	 * @throws Exception the exception
	 */
	public static Script getRevokeScript(KeyWrapper keyList, Channel channel, boolean serverSide) throws Exception {
		ScriptBuilder builder = new ScriptBuilder();
		
		if(serverSide) {
			builder.smallNum(2);
			builder.data(Tools.stringToByte(channel.getPubKeyClient()));
			builder.data(Tools.stringToByte(keyList.getKey()));
			builder.smallNum(2);
			builder.op(174);
		} else {
			builder.smallNum(2);
			builder.data(Tools.stringToByte(keyList.getKey()));
			builder.data(Tools.stringToByte(channel.getPubKeyServer()));
			builder.smallNum(2);
			builder.op(174);
		}
		return builder.build();
	}
	
	/**
	 * Gets the settlement script sig.
	 *
	 * @param channel the channel
	 * @param signatureClient the signature client
	 * @param signatureServer the signature server
	 * @param secret the secret
	 * @param serverSide the server side
	 * @param paymentToServer the payment to server
	 * @return the settlement script sig
	 * @throws Exception the exception
	 */
	public static Script getSettlementScriptSig(Channel channel, ECDSASignature signatureClient, ECDSASignature signatureServer, String secret, boolean serverSide, boolean paymentToServer) throws Exception {
		ScriptBuilder builder = new ScriptBuilder();

		if(serverSide) {
			if(paymentToServer) {
				if(!SideConstants.RUNS_ON_SERVER)
					throw new Exception("We cannot build this transaction, best you don't even try..");
				builder.data(Tools.stringToByte(secret));
				builder.data(signatureClient.encodeToDER());
				builder.data(signatureServer.encodeToDER());
			} else {
				builder.data(Tools.stringToByte(secret));
				builder.data(signatureClient.encodeToDER());
				builder.data(signatureServer.encodeToDER());
			}
		} else {
			if(paymentToServer) {
				builder.data(Tools.stringToByte(secret));
				builder.data(signatureServer.encodeToDER());
			} else {
				if(SideConstants.RUNS_ON_SERVER)
					throw new Exception("We cannot build this transaction, best you don't even try..");
				builder.data(Tools.stringToByte(secret));
				builder.data(signatureClient.encodeToDER());
				builder.data(signatureServer.encodeToDER());
			}
		}
		return builder.build();
	}
	
	/**
	 * Gets the refund script sig.
	 *
	 * @param channel the channel
	 * @param signatureClient the signature client
	 * @param signatureServer the signature server
	 * @param serverSide the server side
	 * @param paymentToServer the payment to server
	 * @return the refund script sig
	 * @throws Exception the exception
	 */
	public static Script getRefundScriptSig(Channel channel, ECDSASignature signatureClient, ECDSASignature signatureServer, boolean serverSide, boolean paymentToServer) throws Exception {
		ScriptBuilder builder = new ScriptBuilder();

		if(serverSide) {
			if(paymentToServer) {
				if(!SideConstants.RUNS_ON_SERVER)
					throw new Exception("We cannot build this transaction, best you don't even try..");
				builder.data(signatureClient.encodeToDER());
				builder.data(signatureServer.encodeToDER());
			} else {
				builder.data(signatureClient.encodeToDER());
				builder.data(signatureServer.encodeToDER());
			}
		} else {
			if(paymentToServer) {
				builder.data(signatureServer.encodeToDER());
			} else {
				if(SideConstants.RUNS_ON_SERVER)
					throw new Exception("We cannot build this transaction, best you don't even try..");
				builder.data(signatureClient.encodeToDER());
				builder.data(signatureServer.encodeToDER());
			}
		}
		return builder.build();
	}

	/**
	 * Check payment script.
	 *
	 * @param output the output
	 * @param channel the channel
	 * @param keyList the key list
	 * @param R the r
	 * @param serverSide the server side
	 * @param paymentToServer the payment to server
	 * @return true, if successful
	 * @throws Exception the exception
	 */
	public static boolean checkPaymentScript(TransactionOutput output, Channel channel, KeyWrapper keyList, String R, boolean serverSide, boolean paymentToServer) throws Exception {
				
		int amountOfChunks = 0;
		if(serverSide) {
			amountOfChunks = 19;
		} else {
			amountOfChunks = 16;
			
		}
		
		List<ScriptChunk> scriptChunks = output.getScriptPubKey().getChunks();
		
		if(scriptChunks.size() != amountOfChunks)
			return false;
	
		int index = 0;
		for(ScriptChunk chunk : scriptChunks) {
			boolean chunkIsCorrect = false;
			boolean opcode = chunk.isOpCode();
			boolean pushData = chunk.isPushData();
			
			if(serverSide) {
				if(paymentToServer) {
					//serverSide paymentToServer
					if(index == 0) {
						if(opcode) {
							//OP_SIZE
							if(chunk.equalsOpCode(130))
								chunkIsCorrect = true;
						}
					} else if(index == 1) {
						if(pushData) {
							//<20>
							if(chunk.data[0] == 20 && chunk.data.length == 1)
								chunkIsCorrect = true;
						}
					} else if(index == 2) {
						if(opcode) {
							//OP_EQUAL
							if(chunk.equalsOpCode(135))
								chunkIsCorrect = true;
						}
					} else if(index == 3) {
						if(opcode) {
							//OP_IF
							if(chunk.equalsOpCode(99))
								chunkIsCorrect = true;
						}
					} else if(index == 4) {
						if(opcode) {
							//OP_HASH160
							if(chunk.equalsOpCode(169))
								chunkIsCorrect = true;
						}
					} else if(index == 5) {
						if(pushData) {
							//OP_PUSHDATA R
							if(chunk.data.length == 20 && chunk.opcode == 20)
								if(R != null) {
									if(Tools.byteToString(chunk.data).equals(R)) {
										chunkIsCorrect = true;
									} else {
										System.out.println("R is not correct. Should be: "+R+" Is: "+Tools.byteToString(chunk.data));
									}
								} else {
									chunkIsCorrect = true;
								}
						}
						
					} else if(index == 6) {
						if(opcode) {
							//OP_EQUALVERIFY
							if(chunk.equalsOpCode(136))
								chunkIsCorrect = true;
						}
					} else if(index == 7) {
						if(opcode) {
							//OP_2
							if(chunk.equalsOpCode(82))
								chunkIsCorrect = true;
						}
					} else if(index == 8) {
						if(pushData) {
							//OP_PUSHDATA clientPubKey
							if(chunk.data.length == 33 && chunk.opcode == 33)
								if(Arrays.equals(chunk.data, Tools.stringToByte(channel.getPubKeyClient())))
									chunkIsCorrect = true;
						}
					} else if(index == 9) {
						if(pushData) {
							//OP_PUSHDATA serverPubKeyTemp
							if(chunk.data.length == 33 && chunk.opcode == 33)
								if(keyList.checkKey(Tools.byteToString(chunk.data)))
									chunkIsCorrect = true;
		
						}
					} else if(index == 10) {
						if(opcode) {
							//OP_2
							if(chunk.equalsOpCode(82))
								chunkIsCorrect = true;
						}
					} else if(index == 11) {
						if(opcode) {
							//OP_CHECKMULTISIG
							if(chunk.equalsOpCode(174))
								chunkIsCorrect = true;
						}
					} else if(index == 12) {
						if(opcode) {
							//OP_ELSE
							if(chunk.equalsOpCode(103))
								chunkIsCorrect = true;
						}
					} else if(index == 13) {
						if(opcode) {
							//OP_2
							if(chunk.equalsOpCode(82))
								chunkIsCorrect = true;
						}
					} else if(index == 14) {
						if(pushData) {
							//OP_PUSHDATA clientPubKey
							if(chunk.data.length == 33 && chunk.opcode == 33)
								if(Arrays.equals(chunk.data, Tools.stringToByte(channel.getPubKeyClient())))
									chunkIsCorrect = true;
						}
					} else if(index == 15) {
						if(pushData) {
							//OP_PUSHDATA serverPubKeyTemp
							if(chunk.data.length == 33 && chunk.opcode == 33)
								if(keyList.checkKey(Tools.byteToString(chunk.data)))
									chunkIsCorrect = true;
		
						}
					} else if(index == 16) {
						if(opcode) {
							//OP_2
							if(chunk.equalsOpCode(82))
								chunkIsCorrect = true;
						}
					} else if(index == 17) {
						if(opcode) {
							//OP_CHECKMULTISIG
							if(chunk.equalsOpCode(174))
								chunkIsCorrect = true;
						}
					} else if(index == 18) {
						if(opcode) {
							//OP_ENDIF
							if(chunk.equalsOpCode(104))
								return true;
						}
					}
				} else {
					//serverSide !paymentToServer
					if(index == 0) {
						if(opcode) {
							//OP_SIZE
							if(chunk.equalsOpCode(130))
								chunkIsCorrect = true;
						}
					} else if(index == 1) {
						if(pushData) {
							//<20>
							if(chunk.data[0] == 20 && chunk.data.length == 1)
								chunkIsCorrect = true;
						}
					} else if(index == 2) {
						if(opcode) {
							//OP_EQUAL
							if(chunk.equalsOpCode(135))
								chunkIsCorrect = true;
						}
					} else if(index == 3) {
						if(opcode) {
							//OP_IF
							if(chunk.equalsOpCode(99))
								chunkIsCorrect = true;
						}
					} else if(index == 4) {
						if(opcode) {
							//OP_HASH160
							if(chunk.equalsOpCode(169))
								chunkIsCorrect = true;
						}
					} else if(index == 5) {
						if(pushData) {
							//OP_PUSHDATA R
							if(chunk.data.length == 20 && chunk.opcode == 20)
								if(R != null) {
									if(Arrays.equals(chunk.data, Tools.stringToByte(R))) {
										chunkIsCorrect = true;
									} else {
										System.out.println("R is not correct. Should be: "+R+" Is: "+Tools.byteToString(chunk.data));
									}
								} else {
									chunkIsCorrect = true;
								}
						}
						
					} else if(index == 6) {
						if(opcode) {
							//OP_EQUALVERIFY
							if(chunk.equalsOpCode(136))
								chunkIsCorrect = true;
						}
					} else if(index == 7) {
						if(opcode) {
							//OP_2
							if(chunk.equalsOpCode(82))
								chunkIsCorrect = true;
						}
					} else if(index == 8) {
						if(pushData) {
							//OP_PUSHDATA clientPubKey
							if(chunk.data.length == 33 && chunk.opcode == 33)
								if(Arrays.equals(chunk.data, Tools.stringToByte(channel.getPubKeyClient())))
									chunkIsCorrect = true;
						}
					} else if(index == 9) {
						if(pushData) {
							//OP_PUSHDATA serverPubKeyTemp
							if(chunk.data.length == 33 && chunk.opcode == 33)
								if(keyList.checkKey(Tools.byteToString(chunk.data)))
									chunkIsCorrect = true;
		
						}
					} else if(index == 10) {
						if(opcode) {
							//OP_2
							if(chunk.equalsOpCode(82))
								chunkIsCorrect = true;
						}
					} else if(index == 11) {
						if(opcode) {
							//OP_CHECKMULTISIG
							if(chunk.equalsOpCode(174))
								chunkIsCorrect = true;
						}
					} else if(index == 12) {
						if(opcode) {
							//OP_ELSE
							if(chunk.equalsOpCode(103))
								chunkIsCorrect = true;
						}
					} else if(index == 13) {
						if(opcode) {
							//OP_2
							if(chunk.equalsOpCode(82))
								chunkIsCorrect = true;
						}
					} else if(index == 14) {
						if(pushData) {
							//OP_PUSHDATA clientPubKey
							if(chunk.data.length == 33 && chunk.opcode == 33)
								if(Arrays.equals(chunk.data, Tools.stringToByte(channel.getPubKeyClient())))
									chunkIsCorrect = true;
						}
					} else if(index == 15) {
						if(pushData) {
							//OP_PUSHDATA serverPubKeyTemp
							if(chunk.data.length == 33 && chunk.opcode == 33)
								if(keyList.checkKey(Tools.byteToString(chunk.data)))
									chunkIsCorrect = true;
		
						}
					} else if(index == 16) {
						if(opcode) {
							//OP_2
							if(chunk.equalsOpCode(82))
								chunkIsCorrect = true;
						}
					} else if(index == 17) {
						if(opcode) {
							//OP_CHECKMULTISIG
							if(chunk.equalsOpCode(174))
								chunkIsCorrect = true;
						}
					} else if(index == 18) {
						if(opcode) {
							//OP_ENDIF
							if(chunk.equalsOpCode(104))
								return true;
						}
					}
				}
			} else {
				if(paymentToServer) {
					//!serverSide paymentToServer
					if(index == 0) {
						if(opcode) {
							//OP_SIZE
							if(chunk.equalsOpCode(130))
								chunkIsCorrect = true;
						}
					} else if(index == 1) {
						if(pushData) {
							//<20>
							if(chunk.data[0] == 20 && chunk.data.length == 1)
								chunkIsCorrect = true;
						}
					} else if(index == 2) {
						if(opcode) {
							//OP_EQUAL
							if(chunk.equalsOpCode(135))
								chunkIsCorrect = true;
						}
					} else if(index == 3) {
						if(opcode) {
							//OP_IF
							if(chunk.equalsOpCode(99))
								chunkIsCorrect = true;
						}
					} else if(index == 4) {
						if(opcode) {
							//OP_HASH160
							if(chunk.equalsOpCode(169))
								chunkIsCorrect = true;
						}
					} else if(index == 5) {
						if(pushData) {
							//OP_PUSHDATA R
							if(chunk.data.length == 20 && chunk.opcode == 20)
								if(R != null) {
									if(Arrays.equals(chunk.data, Tools.stringToByte(R)))
										chunkIsCorrect = true;
								} else {
									chunkIsCorrect = true;
								}
						}
						
					} else if(index == 6) {
						if(opcode) {
							//OP_EQUALVERIFY
							if(chunk.equalsOpCode(136))
								chunkIsCorrect = true;
						}
					} else if(index == 7) {
						if(pushData) {
							//OP_PUSHDATA serverPubKey
							if(chunk.data.length == 33 && chunk.opcode == 33)
								if(Arrays.equals(chunk.data, Tools.stringToByte(channel.getPubKeyServer())))
									chunkIsCorrect = true;
		
						}
					} else if(index == 8) {
						if(opcode) {
							//OP_CHECKSIG
							if(chunk.equalsOpCode(172))
								chunkIsCorrect = true;
						}
					} else if(index == 9) {
						if(opcode) {
							//OP_ELSE
							if(chunk.equalsOpCode(103))
								chunkIsCorrect = true;
						}
					} else if(index == 10) {
						if(opcode) {
							//OP_2
							if(chunk.equalsOpCode(82))
								chunkIsCorrect = true;
						}
					} else if(index == 11) {
						if(pushData) {
							//OP_PUSHDATA clientPubKeyTemp
							if(chunk.data.length == 33 && chunk.opcode == 33)
								if(keyList.checkKey(Tools.byteToString(chunk.data)))
									chunkIsCorrect = true;
						}
					} else if(index == 12) {
						if(pushData) {
							//OP_PUSHDATA serverPubKey
							if(chunk.data.length == 33 && chunk.opcode == 33)
								if(Arrays.equals(chunk.data, Tools.stringToByte(channel.getPubKeyServer())))
									chunkIsCorrect = true;
		
						}
					} else if(index == 13) {
						if(opcode) {
							//OP_2
							if(chunk.equalsOpCode(82))
								chunkIsCorrect = true;
						}
					} else if(index == 14) {
						if(opcode) {
							//OP_CHECKMULTI
							if(chunk.equalsOpCode(174))
								chunkIsCorrect = true;
						}
					} else if(index == 15) {
						if(opcode) {
							//OP_ENDIF
							if(chunk.equalsOpCode(104))
								return true;
						}
					}
				} else {
					//!serverSide !paymentToServer
					if(index == 0) {
						if(opcode) {
							//OP_SIZE
							if(chunk.equalsOpCode(130))
								chunkIsCorrect = true;
						}
					} else if(index == 1) {
						if(pushData) {
							//<20>
							if(chunk.data[0] == 20 && chunk.data.length == 1)
								chunkIsCorrect = true;
						}
					} else if(index == 2) {
						if(opcode) {
							//OP_EQUAL
							if(chunk.equalsOpCode(135))
								chunkIsCorrect = true;
						}
					} else if(index == 3) {
						if(opcode) {
							//OP_IF
							if(chunk.equalsOpCode(99))
								chunkIsCorrect = true;
						}
					} else if(index == 4) {
						if(opcode) {
							//OP_HASH160
							if(chunk.equalsOpCode(169))
								chunkIsCorrect = true;
						}
					} else if(index == 5) {
						if(pushData) {
							//OP_PUSHDATA R
							if(chunk.data.length == 20 && chunk.opcode == 20)
								if(R != null) {
									if(Arrays.equals(chunk.data, Tools.stringToByte(R)))
										chunkIsCorrect = true;
								} else {
									chunkIsCorrect = true;
								}
						}
					} else if(index == 6) {
						if(opcode) {
							//OP_EQUALVERIFY
							if(chunk.equalsOpCode(136))
								chunkIsCorrect = true;
						}
					} else if(index == 7) {
						if(opcode) {
							//OP_2
							if(chunk.equalsOpCode(82))
								chunkIsCorrect = true;
						}
					} else if(index == 8) {
						if(pushData) {
							//OP_PUSHDATA clientPubKeyTemp
							if(chunk.data.length == 33 && chunk.opcode == 33)
								if(keyList.checkKey(Tools.byteToString(chunk.data)))
									chunkIsCorrect = true;
						}
					} else if(index == 9) {
						if(pushData) {
							//OP_PUSHDATA serverPubKey
							if(chunk.data.length == 33 && chunk.opcode == 33)
								if(Arrays.equals(chunk.data, Tools.stringToByte(channel.getPubKeyServer())))
									chunkIsCorrect = true;
	
						}
					} else if(index == 10) {
						if(opcode) {
							//OP_2
							if(chunk.equalsOpCode(82))
								chunkIsCorrect = true;
						}
					} else if(index == 11) {
						if(opcode) {
							//OP_CHECKMULTISIG
							if(chunk.equalsOpCode(174))
								chunkIsCorrect = true;
						}
					} else if(index == 12) {
						if(opcode) {
							//OP_ELSE
							if(chunk.equalsOpCode(103))
								chunkIsCorrect = true;
						}
					} else if(index == 13) {
						if(pushData) {
							//OP_PUSHDATA serverPubKey
							if(chunk.data.length == 33 && chunk.opcode == 33)
								if(Arrays.equals(chunk.data, Tools.stringToByte(channel.getPubKeyServer())))
									chunkIsCorrect = true;
						}
					} else if(index == 14) {
						if(opcode) {
							//OP_CHECKSIG
							if(chunk.equalsOpCode(172))
								chunkIsCorrect = true;
						}
					} else if(index == 15) {
						if(opcode) {
							//OP_ENDIF
							if(chunk.equalsOpCode(104))
								return true;
						}
					}
				}
			}
			
			if(!chunkIsCorrect) {
				System.out.println("Error in chunk "+index);
				break;
			}
			index++;
		}
		return false;
	}
	
	/**
	 * Gets the rof payment script.
	 *
	 * @param output the output
	 * @return the rof payment script
	 */
	public static String getRofPaymentScript(TransactionOutput output) {
		List<ScriptChunk> scriptChunks = output.getScriptPubKey().getChunks();
		return Tools.byteToString(scriptChunks.get(5).data);
	}
	
	/**
	 * Gets the signature ouf of multisig input.
	 *
	 * @param input the input
	 * @return the signature ouf of multisig input
	 */
	public static ECDSASignature getSignatureOufOfMultisigInput(TransactionInput input) {
		List<ScriptChunk> scriptChunks = input.getScriptSig().getChunks();
		return ECDSASignature.decodeFromDER(scriptChunks.get(1).data);
	}
	
	/**
	 * Gets the pub key of payment script.
	 *
	 * @param output the output
	 * @param serverSide the server side
	 * @param refundTransaction the refund transaction
	 * @return the pub key of payment script
	 */
	public static String getPubKeyOfPaymentScript(TransactionOutput output, boolean serverSide, boolean refundTransaction) {
		List<ScriptChunk> scriptChunks = output.getScriptPubKey().getChunks();
		if(serverSide) {
			if(refundTransaction) {
				return Tools.byteToString(scriptChunks.get(15).data);
			} else {
				return Tools.byteToString(scriptChunks.get(9).data);
			}
		} else {
			return Tools.byteToString(scriptChunks.get(12).data);
		}
	}
	
	/**
	 * Check transaction.
	 *
	 * @param transaction the transaction
	 * @param inputIndex the input index
	 * @param hash the hash
	 * @param amount the amount
	 * @param payoutAddress the payout address
	 * @param locktime the locktime
	 * @throws Exception the exception
	 */
	public static void checkTransaction(Transaction transaction, int inputIndex, Sha256Hash hash, long amount, String payoutAddress, int locktime) throws Exception {
		if(transaction.getInputs().size() != 1)
			throw new Exception("Refund Input Size is not 1");

		
		if(transaction.getOutputs().size() != 1)
			throw new Exception("Refund Output Size is not 1");

		
		if(!Tools.compareHash(transaction.getInput(0).getOutpoint().getHash(), hash))
			throw new Exception("Refund Input Hash is not correct");

		
		if(transaction.getInput(0).getOutpoint().getIndex() != inputIndex)
			throw new Exception("Refund Input Index is not correct");					


		if(transaction.getOutput(0).getValue().value != amount )
			throw new Exception("Refund Output value not correct. Should be: "+amount+" Is: "+transaction.getOutput(0).getValue().value);
		
		
		if(!transaction.getOutput(0).getAddressFromP2PKHScript(Constants.getNetwork()).toString().equals(payoutAddress))
			throw new Exception("Refund Output Address is not correct");
		
		if(locktime > 0) {
			if(!Tools.checkTransactionLockTime(transaction, locktime))
				throw new Exception("Refund LockTime is not correct.");
		}
	}
	
	/**
	 * Checks if is payment to server.
	 *
	 * @param output the output
	 * @return true, if is payment to server
	 */
	public static boolean isPaymentToServer(TransactionOutput output) {
		/**
		 * Hack, that is likely to not work anymore in the future to determine paymentToServer property.
		 * However, if this does not work anymore, there is probably no reason to treat these differently.
		 */
		return output.getScriptPubKey().getChunks().get(8).isOpCode();
	}
	
	/**
	 * Gets the pub keys of channel.
	 *
	 * @param channel the channel
	 * @param serverSide the server side
	 * @param temporary the temporary
	 * @return the pub keys of channel
	 * @throws ScriptException the script exception
	 * @throws SQLException the SQL exception
	 */
	public static ArrayList<String> getPubKeysOfChannel(Channel channel, boolean serverSide, boolean temporary) throws ScriptException, SQLException {
		//TODO: probably some refactoring here..
		ArrayList<String> pubKeys = new ArrayList<String>();
		List<TransactionOutput> outputs;
		if(serverSide) {
			if(temporary) {
				if(channel.getChannelTxServerTemp() != null) {
					outputs = channel.getChannelTxServerTemp().getOutputs();
					pubKeys.add(Tools.byteToString(outputs.get(1).getScriptPubKey().getChunks().get(2).data));
					
					if(outputs.size() > 2) {
						for(int i=2; i<outputs.size(); ++i) {
							pubKeys.add(Tools.byteToString(outputs.get(i).getScriptPubKey().getChunks().get(9).data));
							pubKeys.add(Tools.byteToString(outputs.get(i).getScriptPubKey().getChunks().get(15).data));
						}
					}
					
				}
			} else {
				if(channel.getChannelTxServer() != null) {
					outputs = channel.getChannelTxServer().getOutputs();
					pubKeys.add(Tools.byteToString(outputs.get(1).getScriptPubKey().getChunks().get(2).data));
					
					if(outputs.size() > 2) {
						for(int i=2; i<outputs.size(); ++i) {
							pubKeys.add(Tools.byteToString(outputs.get(i).getScriptPubKey().getChunks().get(9).data));
							pubKeys.add(Tools.byteToString(outputs.get(i).getScriptPubKey().getChunks().get(15).data));
						}
					}
				}
			}
				

		} else {
			if(temporary) {
				if(channel.getChannelTxClientTemp() != null) {
					outputs = channel.getChannelTxClientTemp().getOutputs();
					pubKeys.add(Tools.byteToString(outputs.get(0).getScriptPubKey().getChunks().get(1).data));
					
					if(outputs.size() > 2) {
						for(int i=2; i<outputs.size(); ++i) {
							if(outputs.get(i).getScriptPubKey().getChunks().get(8).isOpCode()) {
								pubKeys.add(Tools.byteToString(outputs.get(i).getScriptPubKey().getChunks().get(11).data));
							} else {
								pubKeys.add(Tools.byteToString(outputs.get(i).getScriptPubKey().getChunks().get(8).data));
							}
						}
					}
					
				}
			} else {
				if(channel.getChannelTxClient() != null) {
					outputs = channel.getChannelTxClient().getOutputs();
					pubKeys.add(Tools.byteToString(outputs.get(0).getScriptPubKey().getChunks().get(1).data));
					
					if(outputs.size() > 2) {
						for(int i=2; i<outputs.size(); ++i) {
							if(outputs.get(i).getScriptPubKey().getChunks().get(8).isOpCode()) {
								pubKeys.add(Tools.byteToString(outputs.get(i).getScriptPubKey().getChunks().get(11).data));
							} else {
								pubKeys.add(Tools.byteToString(outputs.get(i).getScriptPubKey().getChunks().get(8).data));
							}
						}
					}
					
				}
			}
		}
		return pubKeys;
	}
	
	/**
	 * Adds the co signature into input script.
	 *
	 * @param script the script
	 * @param signature the signature
	 * @param serverSide the server side
	 * @return the script
	 */
	public static Script addCoSignatureIntoInputScript(Script script, ECDSASignature signature, boolean serverSide) {
		ScriptBuilder builder = new ScriptBuilder(script);
		if(serverSide) {
			/**
			 * As in our convention, the clientSig is first
			 */
			builder.data(signature.encodeToDER());
		} else {
			/**
			 * Push the clientSig before the serverSig (that is in the script already..)
			 */
			builder.data(1, signature.encodeToDER());
		}
		return builder.build();
		
	}
	
	/**
	 * Gets the multisig input script.
	 *
	 * @param sig the sig
	 * @return the multisig input script
	 */
	public static Script getMultisigInputScript(ECDSASignature sig) {
		ScriptBuilder builder = new ScriptBuilder();
		builder.smallNum(0);
		builder.data(sig.encodeToDER());
		return builder.build();
	}

	/**
	 * Gets the multisig input script.
	 *
	 * @param clientSig the client sig
	 * @param serverSig the server sig
	 * @return the multisig input script
	 */
	public static Script getMultisigInputScript(ECDSASignature clientSig, ECDSASignature serverSig) {
		
		TransactionSignature sig1 = new TransactionSignature(clientSig, SigHash.ALL, false);
		TransactionSignature sig2 = new TransactionSignature(serverSig, SigHash.ALL, false);
		Script s = ScriptBuilder.createMultiSigInputScript(sig1, sig2);
		s = new Script(s.getProgram());
		return s;
		
//		ScriptBuilder builder = new ScriptBuilder();
//		builder.smallNum(0);
//		builder.data(clientSig.encodeToDER());
//		builder.data(serverSig.encodeToDER());
//		return builder.build();
	}
	

	/**
	 * Gets the multisig output script.
	 *
	 * @param channel the channel
	 * @return the multisig output script
	 */
	public static Script getMultisigOutputScript(Channel channel) {
		ScriptBuilder builder = new ScriptBuilder();
		builder.smallNum(2);
		builder.data(Tools.stringToByte(channel.getPubKeyClient()));
		builder.data(Tools.stringToByte(channel.getPubKeyServer()));
		builder.smallNum(2);
		builder.op(174);
		return builder.build();
	}
	
	/**
	 * Gets the payment script.
	 *
	 * @param channel the channel
	 * @param keyList the key list
	 * @param R the r
	 * @param serverSide the server side
	 * @param paymentToServer the payment to server
	 * @return the payment script
	 * @throws Exception the exception
	 */
	public static Script getPaymentScript(Channel channel, KeyWrapper keyList, String R, boolean serverSide, boolean paymentToServer) throws Exception {
		ScriptBuilder builder = new ScriptBuilder();
		
		if(serverSide) {
			if(paymentToServer) {
				builder.op(130);
				builder.data( new byte[] {0x14} );
				builder.op(135);
				builder.op(99);
				builder.op(169);
				builder.data(Tools.stringToByte(R));
				builder.op(136);
				builder.smallNum(2);
				builder.data(Tools.stringToByte(channel.getPubKeyClient()));
				builder.data( Tools.stringToByte( keyList.getKey() ));
				builder.smallNum(2);
				builder.op(174);
				builder.op(103);
				builder.smallNum(2);
				builder.data(Tools.stringToByte(channel.getPubKeyClient()));
				builder.data(Tools.stringToByte( keyList.getKey() ));
				builder.smallNum(2);
				builder.op(174);
				builder.op(104);
			} else {
				builder.op(130);
				builder.data( new byte[] {0x14} );
				builder.op(135);
				builder.op(99);
				builder.op(169);
				builder.data(Tools.stringToByte(R));
				builder.op(136);
				builder.smallNum(2);
				builder.data(Tools.stringToByte(channel.getPubKeyClient()));
				builder.data(Tools.stringToByte( keyList.getKey() ));
				builder.smallNum(2);
				builder.op(174);
				builder.op(103);
				builder.smallNum(2);
				builder.data(Tools.stringToByte(channel.getPubKeyClient()));
				builder.data(Tools.stringToByte( keyList.getKey() ));
				builder.smallNum(2);
				builder.op(174);
				builder.op(104);	
			}
		} else {
			if(paymentToServer) {
				builder.op(130);
				builder.data( new byte[] {0x14} );
				builder.op(135);
				builder.op(99);
				builder.op(169);
				builder.data(Tools.stringToByte(R));
				builder.op(136);
				builder.data(Tools.stringToByte(channel.getPubKeyServer()));
				builder.op(172);
				builder.op(103);
				builder.smallNum(2);
				builder.data(Tools.stringToByte( keyList.getKey() ));
				builder.data(Tools.stringToByte(channel.getPubKeyServer()));
				builder.smallNum(2);
				builder.op(174);
				builder.op(104);
			} else {
				builder.op(130);
				builder.data( new byte[] {0x14} );
				builder.op(135);
				builder.op(99);
				builder.op(169);
				builder.data(Tools.stringToByte(R));
				builder.op(136);
				builder.smallNum(2);
				builder.data(Tools.stringToByte( keyList.getKey() ));
				builder.data(Tools.stringToByte(channel.getPubKeyServer()));
				builder.smallNum(2);
				builder.op(174);
				builder.op(103);
				builder.data(Tools.stringToByte(channel.getPubKeyServer()));
				builder.op(172);
				builder.op(104);
			}
		}
		
		
		
		
		return builder.build();
	}
	
	
	
}
