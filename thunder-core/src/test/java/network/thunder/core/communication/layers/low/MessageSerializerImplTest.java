package network.thunder.core.communication.layers.low;

import network.thunder.core.communication.layer.low.serialisation.MessageSerializerImpl;
import network.thunder.core.communication.layer.low.serialisation.MessageSerializer;
import network.thunder.core.etc.RandomDataMessage;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertTrue;

/**
 * TODO: Maybe add some more different messages here?
 */
public class MessageSerializerImplTest {
    @Test
    public void serializeAndDeserialize () {
        MessageSerializer serializer = new MessageSerializerImpl();
        RandomDataMessage message = new RandomDataMessage();

        byte[] serializedMessage = serializer.serializeMessage(message);
        RandomDataMessage deserializedMessage = (RandomDataMessage) serializer.deserializeMessage(serializedMessage);

        assertTrue(Arrays.equals(deserializedMessage.data, message.data));
    }
}