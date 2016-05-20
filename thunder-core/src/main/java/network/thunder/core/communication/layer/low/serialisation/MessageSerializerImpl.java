package network.thunder.core.communication.layer.low.serialisation;

import com.google.gson.*;
import network.thunder.core.communication.layer.Message;
import network.thunder.core.etc.Tools;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;

public class MessageSerializerImpl implements MessageSerializer {

    Gson gsonS = new GsonBuilder()
            .registerTypeHierarchyAdapter(Message.class, new InterfaceAdapter<Message>())
            .create();

    Gson gsonD = new GsonBuilder()
            .registerTypeAdapter(byte[].class, new ByteArrayToBase64TypeAdapter())
            .registerTypeAdapter(Message.class, new InterfaceAdapter<Message>()).create();

    @Override
    public byte[] serializeMessage (Message message) {
        try {
            String json = gsonS.toJson(message);
            byte[] data = json.getBytes("UTF-8");

            return data;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Message deserializeMessage (byte[] data) {
        try {
            String json = new String(data, "UTF-8");
            Message message = gsonD.fromJson(json, Message.class);

            return message;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    static class ByteArrayToBase64TypeAdapter implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {
        @Override
        public byte[] deserialize (JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return Tools.stringToByte(json.getAsString());
        }

        @Override
        public JsonElement serialize (byte[] src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(Tools.byteToString(src));
        }
    }

}
