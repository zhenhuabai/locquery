package common;

import java.util.logging.Logger;

/**
 * Created by 白振华 on 2017/1/7.
 */
public abstract class Loggable {
    protected Logger Log = null;
    public Loggable()
    {
        Log = Logger.getLogger(this.getClass().getName());
    }
}
