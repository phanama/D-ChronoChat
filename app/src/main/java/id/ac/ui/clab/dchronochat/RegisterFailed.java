package id.ac.ui.clab.dchronochat;

import net.named_data.jndn.Name;
import net.named_data.jndn.OnRegisterFailed;

/**
 * Created by yudiandreanp on 08/05/16.
 */
public class RegisterFailed implements OnRegisterFailed {
    public final void
    onRegisterFailed(Name prefix)
    {
        System.out.println("Register failed for prefix " + prefix.toUri());
    }

    public final static OnRegisterFailed onRegisterFailed = new RegisterFailed();
}
