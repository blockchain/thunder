package network.thunder.core.etc;

import io.netty.channel.embedded.EmbeddedChannel;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;

public class TestTools {
    public static void exchangeMessages (EmbeddedChannel from, EmbeddedChannel to) {
        Object message = from.readOutbound();
        System.out.println(message);
        if (message != null) {
            to.writeInbound(message);
        }
    }

    public static void exchangeMessages (EmbeddedChannel from, EmbeddedChannel to, Class expectedMessage) {
        Object message = from.readOutbound();
        assertThat(message, instanceOf(expectedMessage));
        if (message != null) {
            to.writeInbound(message);
        }
    }

    public static void exchangeMessagesDuplex (EmbeddedChannel from, EmbeddedChannel to) {
        exchangeMessages(from, to);
        exchangeMessages(to, from);
    }
}
