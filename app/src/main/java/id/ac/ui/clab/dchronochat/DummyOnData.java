package id.ac.ui.clab.dchronochat;

import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.OnData;

/**
 * Created by yudiandreanp on 08/05/16.
 */
public class DummyOnData implements OnData {
    public final void
    onData(Interest interest, Data data) {}

    public final static OnData onData = new DummyOnData();
}
