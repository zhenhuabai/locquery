package com.huleibo;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.util.AbstractCollection;

/**
 * Created by 白振华 on 2017/2/17.
 * The class for other server to inherit
 */
public abstract class LocApp extends AbstractVerticle implements SignalHandler {
    protected EventBus eb;
    protected void installSignal(){
        Signal.handle(new Signal("TERM"), this);
    }
}
