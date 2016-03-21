package network.thunder.core.communication.layer.low.serialisation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import network.thunder.core.communication.layer.Message;

import java.io.UnsupportedEncodingException;

public class MessageSerializerImpl implements MessageSerializer {
    @Override
    public byte[] serializeMessage (Message message) {
        try {
            Gson gson = new GsonBuilder().registerTypeHierarchyAdapter(Message.class, new InterfaceAdapter<Message>()).create();

            String json = gson.toJson(message);
            byte[] data = json.getBytes("UTF-8");

            return data;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Message deserializeMessage (byte[] data) {
        try {
            Gson gson = new GsonBuilder().registerTypeAdapter(Message.class, new InterfaceAdapter<Message>()).create();

            String json = new String(data, "UTF-8");

            Message message = gson.fromJson(json, Message.class);

            return message;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}
