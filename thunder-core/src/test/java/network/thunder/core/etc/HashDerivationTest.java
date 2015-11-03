package network.thunder.core.etc;

import network.thunder.core.lightning.HashDerivation;
import network.thunder.core.lightning.RevocationHash;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Created by matsjerratsch on 03/11/15.
 */
public class HashDerivationTest {

    @Test
    public void shouldCalculateCorrectMasterSeed () {
        //Let's start with a given seed
        //Values below are calculated using http://www.fileformat.info/tool/hash.htm?hex=010649653115246623551233652115421336a8b6
        byte[] seed = Tools.hexStringToByteArray("010649653115246623551233652115421336a8b6");

        RevocationHash revocationHash = HashDerivation.calculateRevocationHash(seed, 1, 0);

        assertEquals(revocationHash.getChild(), 0);
        assertEquals(revocationHash.getDepth(), 1);
        assertTrue(Arrays.equals(revocationHash.getSecret(), Tools.hexStringToByteArray("087d3e3f876d41fe243ca517809aa4a662eccba2")));
        assertNull(revocationHash.getSecretHash());
    }

    @Test
    public void shouldCalculateCorrectRevocationHash () {
        //Let's start with a given seed
        //Values below are calculated using http://www.fileformat.info/tool/hash.htm?hex=010649653115246623551233652115421336a8b6
        byte[] seed = Tools.hexStringToByteArray("010649653115246623551233652115421336a8b6");

        RevocationHash revocationHash = HashDerivation.calculateRevocationHash(seed, 1, 1);

        assertEquals(revocationHash.getChild(), 1);
        assertEquals(revocationHash.getDepth(), 1);
        assertTrue(Arrays.equals(revocationHash.getSecret(), Tools.hexStringToByteArray("7f66cee7aea711cc30f0ea9fd0612d8e63c53beb")));
        assertTrue(Arrays.equals(revocationHash.getSecretHash(), Tools.hexStringToByteArray("e7e24faa81ef3ae383464031cba965a05f6f4aa6")));

    }

    @Test
    public void shouldFindCorrectSecretWithBruteForce () {
        byte[] seed = Tools.hexStringToByteArray("010649653115246623551233652115421336a8b6");

        RevocationHash masterHash = HashDerivation.calculateRevocationHash(seed, 0, 0);

        RevocationHash targetHash = HashDerivation.calculateRevocationHash(seed, 250, 250);

        RevocationHash foundHash = HashDerivation.bruteForceHash(masterHash.getSecret(), targetHash.getSecretHash(), 500, 500);

        assertEquals(foundHash, targetHash);
    }

    @Test
    public void shouldTimeoutWhileTryingToBruteForce () {
        byte[] seed = Tools.hexStringToByteArray("010649653115246623551233652115421336a8b6");

        RevocationHash masterHash = HashDerivation.calculateRevocationHash(seed, 0, 0);

        RevocationHash targetHash = HashDerivation.calculateRevocationHash(seed, 250, 250);

        RevocationHash foundHash = HashDerivation.bruteForceHash(masterHash.getSecret(), targetHash.getSecretHash(), 100, 100);

        assertNull(foundHash);
    }
}