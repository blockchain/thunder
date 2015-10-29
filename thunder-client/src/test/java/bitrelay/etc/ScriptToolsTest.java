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
package bitrelay.etc;

public class ScriptToolsTest {

    //	@BeforeClass
    //	public static void setUpBeforeClass() throws Exception {
    //	}
    //
    //	@AfterClass
    //	public static void tearDownAfterClass() throws Exception {
    //	}
    //
    //	@Before
    //	public void setUp() throws Exception {
    //	}
    //
    //	@After
    //	public void tearDown() throws Exception {
    //	}
    //
    //	@Test
    //	public final void testCheckRevokeScriptServerSide() throws Exception {
    //
    //		KeyWrapper keyList = MockObjects.getKeyWrapper();
    //		Channel channel = MockObjects.getChannel();
    //
    //		Script script = ScriptTools.getRevokeScript(keyList, channel, true);
    //		TransactionOutput output = new TransactionOutput(Constants.getNetwork(), null, Coin.valueOf(1000), script.getProgram());
    //		assertTrue(ScriptTools.checkRevokeScript(output, channel, keyList, true));
    //
    //		keyList = MockObjects.getKeyWrapper();
    //		channel = MockObjects.getChannel();
    //
    //		script = ScriptTools.getRevokeScript(keyList, channel, false);
    //		output = new TransactionOutput(Constants.getNetwork(), null, Coin.valueOf(1000), script.getProgram());
    //		assertFalse(ScriptTools.checkRevokeScript(output, channel, keyList, true));
    //	}
    //
    //
    //
    //	@Test
    //	public final void testCheckRevokeScriptClientSide() throws Exception {
    //
    //		KeyWrapper keyList = MockObjects.getKeyWrapper();
    //		Channel channel = MockObjects.getChannel();
    //
    //		Script script = ScriptTools.getRevokeScript(keyList, channel, false);
    //		TransactionOutput output = new TransactionOutput(Constants.getNetwork(), null, Coin.valueOf(1000), script.getProgram());
    //		assertTrue(ScriptTools.checkRevokeScript(output, channel, keyList, false));
    //
    //		keyList = MockObjects.getKeyWrapper();
    //		channel = MockObjects.getChannel();
    //
    //		script = ScriptTools.getRevokeScript(keyList, channel, true);
    //		output = new TransactionOutput(Constants.getNetwork(), null, Coin.valueOf(1000), script.getProgram());
    //		assertFalse(ScriptTools.checkRevokeScript(output, channel, keyList, false));
    //	}
    //
    //
    //
    //	@Test
    //	public final void testCheckPaymentScriptToServer() throws Exception {
    //
    //		KeyWrapper keyList = MockObjects.getKeyWrapper();
    //		Channel channel = MockObjects.getChannel();
    //		Payment payment = MockObjects.getPaymentToServer();
    //
    //		Script script = ScriptTools.getPaymentScript(channel, keyList, payment.getSecretHash(), true, payment.paymentToServer);
    //		TransactionOutput output = new TransactionOutput(Constants.getNetwork(), null, Coin.valueOf(1000), script.getProgram());
    //		assertTrue(ScriptTools.checkPaymentScript(output, channel, keyList, payment.getSecretHash(), true, payment.paymentToServer));
    //
    //		keyList = MockObjects.getKeyWrapper();
    //		script = ScriptTools.getPaymentScript(channel, keyList, payment.getSecretHash(), false, payment.paymentToServer);
    //		output = new TransactionOutput(Constants.getNetwork(), null, Coin.valueOf(1000), script.getProgram());
    //		assertFalse(ScriptTools.checkPaymentScript(output, channel, keyList, payment.getSecretHash(), true, payment.paymentToServer));
    //
    //		keyList = MockObjects.getKeyWrapper();
    //		script = ScriptTools.getPaymentScript(channel, keyList, payment.getSecretHash(), false, payment.paymentToServer);
    //		output = new TransactionOutput(Constants.getNetwork(), null, Coin.valueOf(1000), script.getProgram());
    //		assertTrue(ScriptTools.checkPaymentScript(output, channel, keyList, payment.getSecretHash(), false, payment.paymentToServer));
    //
    //		keyList = MockObjects.getKeyWrapper();
    //		script = ScriptTools.getPaymentScript(channel, keyList, payment.getSecretHash(), true, payment.paymentToServer);
    //		output = new TransactionOutput(Constants.getNetwork(), null, Coin.valueOf(1000), script.getProgram());
    //		assertFalse(ScriptTools.checkPaymentScript(output, channel, keyList, payment.getSecretHash(), false, payment.paymentToServer));
    //
    //	}
    //
    //	@Test
    //	public final void testCheckPaymentScriptToClient() throws Exception {
    //
    //		KeyWrapper keyList = MockObjects.getKeyWrapper();
    //		Channel channel = MockObjects.getChannel();
    //		Payment payment = MockObjects.getPaymentToClient();
    //
    //		Script script = ScriptTools.getPaymentScript(channel, keyList, payment.getSecretHash(), true, payment.paymentToServer);
    //		TransactionOutput output = new TransactionOutput(Constants.getNetwork(), null, Coin.valueOf(1000), script.getProgram());
    //		assertTrue(ScriptTools.checkPaymentScript(output, channel, keyList, payment.getSecretHash(), true, payment.paymentToServer));
    //
    //		keyList = MockObjects.getKeyWrapper();
    //		script = ScriptTools.getPaymentScript(channel, keyList, payment.getSecretHash(), false, payment.paymentToServer);
    //		output = new TransactionOutput(Constants.getNetwork(), null, Coin.valueOf(1000), script.getProgram());
    //		assertFalse(ScriptTools.checkPaymentScript(output, channel, keyList, payment.getSecretHash(), true, payment.paymentToServer));
    //
    //		keyList = MockObjects.getKeyWrapper();
    //		script = ScriptTools.getPaymentScript(channel, keyList, payment.getSecretHash(), false, payment.paymentToServer);
    //		output = new TransactionOutput(Constants.getNetwork(), null, Coin.valueOf(1000), script.getProgram());
    //		assertTrue(ScriptTools.checkPaymentScript(output, channel, keyList, payment.getSecretHash(), false, payment.paymentToServer));
    //
    //		keyList = MockObjects.getKeyWrapper();
    //		script = ScriptTools.getPaymentScript(channel, keyList, payment.getSecretHash(), true, payment.paymentToServer);
    //		output = new TransactionOutput(Constants.getNetwork(), null, Coin.valueOf(1000), script.getProgram());
    //		assertFalse(ScriptTools.checkPaymentScript(output, channel, keyList, payment.getSecretHash(), false, payment.paymentToServer));
    //
    //	}
    //
    //	@Test
    //	public final void testGetRofPaymentScript() throws Exception {
    //		KeyWrapper keyList = MockObjects.getKeyWrapper();
    //		Channel channel = MockObjects.getChannel();
    //		Payment payment = MockObjects.getPaymentToClient();
    //
    //		Script script = ScriptTools.getPaymentScript(channel, keyList, payment.getSecretHash(), true, payment.paymentToServer);
    //		TransactionOutput output = new TransactionOutput(Constants.getNetwork(), null, Coin.valueOf(1000), script.getProgram());
    //
    //
    //		String R = ScriptTools.getRofPaymentScript(output);
    //		assertEquals(payment.getSecretHash(), R);
    //	}
    //
    //	@Test
    //	public final void testGetSignatureOufOfMultisigInput() {
    ////		fail("Not yet implemented"); // TODO
    //	}
    //
    //	@Test
    //	public final void testGetPubKeyOfPaymentScript() {
    ////		fail("Not yet implemented"); // TODO
    //	}
    //
    //	@Test
    //	public final void testCheckTransaction() {
    ////		fail("Not yet implemented"); // TODO
    //	}
    //
    //	@Test
    //	public final void testIsPaymentToServer() {
    ////		fail("Not yet implemented"); // TODO
    //	}
    //
    //	@Test
    //	public final void testGetPubKeysOfChannel() {
    ////		fail("Not yet implemented"); // TODO
    //	}
    //
    //	@Test
    //	public final void testAddCoSignatureIntoInputScript() {
    ////		fail("Not yet implemented"); // TODO
    //	}
    //
    //	@Test
    //	public final void testGetMultisigInputScriptECDSASignature() {
    ////		fail("Not yet implemented"); // TODO
    //	}
    //
    //	@Test
    //	public final void testGetMultisigInputScriptECDSASignatureECDSASignature() {
    ////		fail("Not yet implemented"); // TODO
    //	}
    //
    //	@Test
    //	public final void testGetMultisigOutputScript() {
    ////		fail("Not yet implemented"); // TODO
    //	}
    //
    //	@Test
    //	public final void testGetPaymentScript() {
    ////		fail("Not yet implemented"); // TODO
    //	}

}
