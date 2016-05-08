package id.ac.ui.clab.dchronochat;

import net.named_data.jndn.Interest;
import net.named_data.jndn.OnTimeout;

/**
 * Created by yudiandreanp on 08/05/16.
 */
public class ChatTimeout implements OnTimeout {
    public final void
    onTimeout(Interest interest) {
        System.out.println("Timeout waiting for chat data");
    }

    public final static OnTimeout onTimeout = new ChatTimeout();
}
