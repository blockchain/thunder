package network.thunder.core.etc;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ToolsTest {

    @Test
    public void testCorrectBIP69Ordering () {
        //Lets just use some random tx that actually complies with BIP69 ( d82bec54384bb6068c3a79aef25711e85676385b8d5bf538741c320100009a41 )
        byte[] payload = Tools.hexStringToByteArray
                ("0100000004203107c7bd5e6b3df3e0295d8d0f1b6a978edbb624373660963c1cd9db133f50000000006a473044022020abc0c700b162f0936fdf211cc337475aab1bf1128396272d3fd240dd7aacef02201399da06807c44e9a844378b9b5aa12b22481192f8ff97ff26b674d8fa23c985012103dcf2ca4676ad26c62065084f52557443f20bd3798657733dac7ef82260f3d926ffffffffe51a564020381759b7cd183c3e3b4612eea89b5650bc349e8b335a698e4d3d8b000000006b483045022100c1a48fa21dd5a0572f3a79c38f45e1613303867ac9134a726c67244e07b3ba3602207baa88d9781271002f0ef7cf743c36b23907996516c96add6ef139863c8b0068012103dcf2ca4676ad26c62065084f52557443f20bd3798657733dac7ef82260f3d926ffffffff5c5b39eff2fca65c74a4eba65706bd07a6fb6a9ebe92f580466f1fd426dbd5b3000000006b483045022100a715e8ab5685edfb2bf73b9e2b9735fb790bacc8b301c1c6ae8e991eadd1262002202d1aaa86f83d7d84444a0590a4a991f4244668af298e0e486bbdcacc90f95e04012103dcf2ca4676ad26c62065084f52557443f20bd3798657733dac7ef82260f3d926ffffffffbc432a3bde714940ae4a46cef8d8eed1c31e32fc7e8bc3e83a9c62da129f57db000000006a473044022017d18356ab1505f3376a827a99c63390a5f1de06304036b555361b99e96cf12e022057e59fd7934a23d1e67081adccda03323b21f09c1573caadd5df07e5f983f878012103dcf2ca4676ad26c62065084f52557443f20bd3798657733dac7ef82260f3d926ffffffff028d352100000000001976a914677eaad53b1737c2aa8d6bcfc9d7ee71348d3b7f88ac489b3a0a000000001976a9143e0174b0297ac03e03992bb9e23405ff9ed39e8f88ac00000000");

        Transaction correctTransaction = new Transaction(Constants.getNetwork(), payload);
        Transaction shuffledTransaction = shuffleTransaction(correctTransaction);
        Transaction appliedTransaction = Tools.applyBIP69(shuffledTransaction);

        assertEquals(correctTransaction.getHash(), appliedTransaction.getHash());

    }

    private static Transaction shuffleTransaction (Transaction transaction) {
        Transaction shuffledTransaction = new Transaction(Constants.getNetwork());

        List<TransactionInput> shuffledInputs = new ArrayList<>(transaction.getInputs());
        List<TransactionOutput> shuffledOutputs = new ArrayList<>(transaction.getOutputs());

        Collections.shuffle(shuffledInputs);
        Collections.shuffle(shuffledOutputs);
        for (TransactionInput input : shuffledInputs) {
            shuffledTransaction.addInput(input);
        }
        for (TransactionOutput output : shuffledOutputs) {
            shuffledTransaction.addOutput(output);
        }
        return shuffledTransaction;
    }

}