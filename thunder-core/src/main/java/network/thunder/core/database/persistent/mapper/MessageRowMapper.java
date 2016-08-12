package network.thunder.core.database.persistent.mapper;

import network.thunder.core.communication.layer.DIRECTION;
import network.thunder.core.communication.layer.Message;
import network.thunder.core.communication.layer.MessageWrapper;
import network.thunder.core.communication.layer.high.NumberedMessage;
import network.thunder.core.communication.layer.low.serialisation.MessageSerializer;
import network.thunder.core.communication.layer.low.serialisation.MessageSerializerImpl;
import network.thunder.core.etc.Tools;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import static network.thunder.core.database.persistent.DBTableNames.MESSAGE;

public class MessageRowMapper implements ResultSetMapper<MessageWrapper> {
    public static final MessageRowMapper INSTANCE = new MessageRowMapper(new MessageSerializerImpl());

    MessageSerializer messageSerializer;

    public MessageRowMapper (MessageSerializer messageSerializer) {
        this.messageSerializer = messageSerializer;
    }

    @Override
    public MessageWrapper map (int index, ResultSet r, StatementContext ctx) throws SQLException {

        byte[] messageBytes = r.getBytes(MESSAGE + ".message_data");

        Message message = messageSerializer.deserializeMessage(messageBytes);

        DIRECTION direction;
        if (Tools.intToBool(r.getInt(MESSAGE + ".sent"))) {
            direction = DIRECTION.SENT;
        } else {
            direction = DIRECTION.RECEIVED;
        }

        int timestamp = r.getInt(MESSAGE + ".timestamp");

        //TODO I think thats part of the serialisation already...
//        if(message instanceof AckableMessage) {
//            AckableMessage ackableMessage = (AckableMessage) message;
//            ackableMessage.setMessageNumber(r.getInt(MESSAGE+".message_id"));
//        }
//
//        if(message instanceof AckMessage) {
//            AckableMessage ackableMessage = (AckableMessage) message;
//            ackableMessage.setMessageNumber(r.getInt(MESSAGE+".message_id"));
//        }
//
//        if(message instanceof AckableMessage) {
//            AckableMessage ackableMessage = (AckableMessage) message;
//            ackableMessage.setMessageNumber(r.getInt(MESSAGE+".message_id"));
//        }

        return new MessageWrapper(message, timestamp, direction);
    }

    public void bindChannelToQuery (SQLStatement update, MessageWrapper messageWrapper) {

        update.bind("message_data", messageSerializer.serializeMessage(messageWrapper.getMessage()));

        if (messageWrapper.getMessage() instanceof NumberedMessage) {
            NumberedMessage numberedMessage = (NumberedMessage) messageWrapper.getMessage();
            update.bind("message_id", numberedMessage.getMessageNumber());
        }

        update
                .bind("timestamp", messageWrapper.getTimestamp())
                .bind("sent", Tools.boolToInt(messageWrapper.getDirection() == DIRECTION.SENT))
                .bind("message_class", messageWrapper.getMessage().getMessageType());
    }

}
