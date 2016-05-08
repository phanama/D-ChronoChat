package id.ac.ui.clab.dchronochat;

import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnTimeout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This repeatedly calls itself after a timeout to send a heartbeat message
 * (chat message type HELLO).
 * This method has an "interest" argument because we use it as the onTimeout
 * for Face.expressInterest.
 */
public class Heartbeat implements OnTimeout {

    private final ArrayList messageCache = new ArrayList(); // of CachedMessage

    public final void onTimeout(Interest interest) {
        if (messageCache.size() == 0)
            messageCacheAppend(ChatMessage.ChatMessageType.JOIN, "xxx");

        try {
            sync_.publishNextSequenceNo();
        } catch (IOException | SecurityException ex) {
            Logger.getLogger(Chat.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        messageCacheAppend(ChatMessage.ChatMessageType.HELLO, "xxx");

        // Call again.
        // TODO: Are we sure using a "/local/timeout" interest is the best future call approach?
        Interest timeout = new Interest(new Name("/local/timeout"));
        timeout.setInterestLifetimeMilliseconds(60000);
        try {
            face.expressInterest(timeout, DummyOnData.onData, heartbeat);
        } catch (IOException ex) {
            Logger.getLogger(DChronoChat.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
