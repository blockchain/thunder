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
package network.thunder.client;

import network.thunder.client.api.PaymentRequest;
import network.thunder.client.api.ThunderContext;
import network.thunder.client.communications.ClientTools;
import network.thunder.client.database.MySQLConnection;
import network.thunder.client.database.objects.Channel;
import network.thunder.client.database.objects.Payment;
import network.thunder.client.etc.Constants;
import network.thunder.client.etc.SideConstants;
import network.thunder.client.etc.Tools;
import network.thunder.client.wallet.KeyChain;

import network.thunder.client.wallet.TransactionStorage;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.utils.BriefLogFormatter;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;

public class TestCase {
	
	
	/**
	 * Create a new channel
	 */

	
	KeyChain keychain;
	
	public static String channel1 = Tools.byteToString(Tools.stringToByte58("oVddFMVCgFRyQq7ouzHy9DW82C3sRgZZr8J7rQdDbD9G"));
	public static String channel2 = Tools.byteToString(Tools.stringToByte58("kBze7cTJN8i65Xfah56DF2BhAFyTnQV7vq1Hs1N591YU"));
	
	public static boolean createNewChannels = false;
	
    public static void main(String[] args) throws Exception {
        BriefLogFormatter.init();


//        for (int i=0; i<10; ++i)
            testPaymentsWithUpdates(10);
//        testPaymentsWithRefunds(10);



        System.exit(-1);
    }

    /**
     * Have channel 1 send money to channel 2
     *
     * @param amount
     * @throws Exception
     */
    static void testPaymentsWithUpdates(int amount) throws Exception {

        WalletAppKit kit1 = new WalletAppKit(Constants.getNetwork(), new File("."), SideConstants.WALLET_FILE+"1");
        WalletAppKit kit2 = new WalletAppKit(Constants.getNetwork(), new File("."), SideConstants.WALLET_FILE+"2");

        try {

            kit1.startAndWait();
            kit2.startAndWait();

            System.out.println(kit1.wallet().toString(false, false, false, null));
            System.out.println(kit2.wallet().toString(false, false, false, null));

            ThunderContext context1 = ThunderContext.init(kit1.wallet(), kit1.peerGroup(), 1, true);
            ThunderContext context2 = ThunderContext.init(kit2.wallet(), kit2.peerGroup(), 2, true);

            context1.setErrorListener(new ThunderContext.ErrorListener() {
                @Override
                public void error(String error) {
                    System.exit(0);
                }
            });
            context2.setErrorListener(new ThunderContext.ErrorListener() {
                @Override
                public void error(String error) {
                    System.exit(0);
                }
            });

            context1.openChannel(100000, 100000, 30);
            context2.openChannel(100000, 100000, 30);

            context1.waitUntilReady();
            context2.waitUntilReady();

            int i=0;

            i=0;
            while(i<amount) {
                PaymentRequest newPayment = context1.getPaymentReceiveRequest(1000);
                context2.makePayment(1000, newPayment.getAddress());
                i++;

            }

            context1.waitUntilReady();
            context2.waitUntilReady();


            i=0;
            while(i<amount) {
                PaymentRequest newPayment = context2.getPaymentReceiveRequest(1000);
                context1.makePayment(1000, newPayment.getAddress());
                i++;

            }

            context1.waitUntilReady();
            context2.waitUntilReady();

            i=0;
            while(i<amount) {
                PaymentRequest newPayment1 = context1.getPaymentReceiveRequest(1000);
                PaymentRequest newPayment2 = context2.getPaymentReceiveRequest(1000);
                context1.makePayment(1000, newPayment2.getAddress());
                context2.makePayment(1000, newPayment1.getAddress());
                i++;

            }

            context1.waitUntilReady();
            context2.waitUntilReady();

            context1.updateChannel();
            context2.updateChannel();

            context1.waitUntilReady();
            context2.waitUntilReady();

//            Thread.sleep(200);

            context1.closeChannel();
            context2.closeChannel();

            context1.waitUntilReady();
            context2.waitUntilReady();

        } finally {
            kit1.stopAndWait();
            kit2.stopAndWait();
        }

    }

    /**
     * Have channel 1 send money to channel 2
     *
     * @param amount
     * @throws Exception
     */
    static void testPaymentsWithRefunds(int amount) throws Exception {

        WalletAppKit kit1 = new WalletAppKit(Constants.getNetwork(), new File("."), SideConstants.WALLET_FILE+"1");
        WalletAppKit kit2 = new WalletAppKit(Constants.getNetwork(), new File("."), SideConstants.WALLET_FILE+"2");

        try {

            kit1.startAndWait();
            kit2.startAndWait();

            System.out.println(kit1.wallet().toString(false, false, false, null));
            System.out.println(kit2.wallet().toString(false, false, false, null));

            ThunderContext context1 = ThunderContext.init(kit1.wallet(), kit1.peerGroup(), 1, true);
            ThunderContext context2 = ThunderContext.init(kit2.wallet(), kit2.peerGroup(), 2, true);

            context1.setErrorListener(new ThunderContext.ErrorListener() {
                @Override
                public void error(String error) {
                    System.exit(0);
                }
            });
            context2.setErrorListener(new ThunderContext.ErrorListener() {
                @Override
                public void error(String error) {
                    System.exit(0);
                }
            });

            context1.openChannel(100000, 100000, 30);
            context2.openChannel(100000, 100000, 30);

            context1.waitUntilReady();
            context2.waitUntilReady();

            int i=0;

            i=0;
            while(i<amount) {
                PaymentRequest newPayment = context1.getPaymentReceiveRequest(1000);
                String changedAddress = newPayment.getAddress().substring(0, 30) + "AAAA" + newPayment.getAddress().substring(34);
                context2.makePayment(1000, changedAddress);
                i++;
//                Thread.sleep(3000);
            }

            i=0;
            while(i<amount) {
                PaymentRequest newPayment = context2.getPaymentReceiveRequest(1000);
                String changedAddress = newPayment.getAddress().substring(0, 30) + "AAAA" + newPayment.getAddress().substring(34);
                context1.makePayment(1000, changedAddress);
                i++;
//                Thread.sleep(3000);
            }

            i=0;
            while(i<amount) {
                PaymentRequest newPayment = context1.getPaymentReceiveRequest(1000);
                context2.makePayment(1000, newPayment.getAddress());
                i++;
//                Thread.sleep(3000);
            }

            i=0;
            while(i<amount) {
                PaymentRequest newPayment = context2.getPaymentReceiveRequest(1000);
                context1.makePayment(1000, newPayment.getAddress());
                i++;
//                Thread.sleep(3000);
            }


//            Thread.sleep(2000);

            context1.closeChannel();
            context2.closeChannel();

            context1.waitUntilReady();
            context2.waitUntilReady();


        } finally {
            kit1.stopAndWait();
            kit2.stopAndWait();
        }

    }
    

}
