package id.ac.ui.clab.dchronochat;

/**
 * Created by yudiandreanp on 08/05/16.
 */

import id.ac.ui.clab.dchronochat.ChatbufProto.ChatMessage;

public class CachedMessage {
    public CachedMessage
            (long sequenceNo, ChatMessage.ChatMessageType messageType, String message, double time)
    {
        sequenceNo_ = sequenceNo;
        messageType_ = messageType;
        message_ = message;
        time_ = time;
    }

    public final long
    getSequenceNo() { return sequenceNo_; }

    public final ChatMessage.ChatMessageType
    getMessageType() { return messageType_; }

    public final String
    getMessage() { return message_; }

    public final double
    getTime() { return time_; }

    private final long sequenceNo_;
    private final ChatMessage.ChatMessageType messageType_;
    private final String message_;
    private final double time_;
};
