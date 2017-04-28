package network.thunder.core.etc;

import network.thunder.core.communication.layer.high.RevocationHash;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.Random;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RevocationHashTest {

    @Test
    public void shouldFailBecauseOfWrongSecret () throws Exception {
        byte[] secret = new byte[20];
        Random r = new SecureRandom();
        r.nextBytes(secret);

        RevocationHash revocationHash = new RevocationHash(10, 10, secret, secret);
        assertFalse(revocationHash.check());
    }

    @Test
    public void shouldPassBecauseCorrectSecret () throws Exception {
        byte[] secret = new byte[20];
        Random r = new SecureRandom();
        r.nextBytes(secret);
        byte[] hash = Tools.hashSecret(secret);

        RevocationHash revocationHash = new RevocationHash(10, 10, secret, hash);
        assertTrue(revocationHash.check());
    }

    @Test
    public void shouldPassBecauseOfNewMaster () throws Exception {
        byte[] secret = new byte[20];
        Random r = new SecureRandom();
        r.nextBytes(secret);

        RevocationHash revocationHash = new RevocationHash(0, 0, secret, secret);
        assertTrue(revocationHash.check());
    }
}