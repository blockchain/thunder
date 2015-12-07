package network.thunder.core.communication.objects.messages.impl;

import network.thunder.core.communication.objects.messages.interfaces.helper.MessageSerializer;
import network.thunder.core.etc.RandomDataMessage;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertTrue;

/**
 * Created by matsjerratsch on 07/12/2015.
 * <p>
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