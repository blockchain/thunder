package network.thunder.core.etc;

import network.thunder.core.communication.Message;

import java.util.Arrays;
import java.util.Random;

public class RandomDataMessage implements Message {

    public RandomDataMessage () {
        //Create some gibberish to parse through them
        byte[] message = new byte[1024];
        Random r = new Random();
        r.nextBytes(message);
        data = message;
    }

    public byte[] data;

    @Override
    public void verify () {

    }

    @Override
    public boolean equals (Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RandomDataMessage that = (RandomDataMessage) o;

        return Arrays.equals(data, that.data);

    }

    @Override
    public int hashCode () {
        return data != null ? Arrays.hashCode(data) : 0;
    }
}
