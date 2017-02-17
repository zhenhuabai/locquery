package com.huleibo;

import common.Config;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import locutil.UserLocation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Created by 白振华 on 2017/2/8.
 */
@RunWith(VertxUnitRunner.class)
public class LocationManagerTest {
    private long UID = 1001001;

    @Test
    public void testCmdHandler(TestContext context) throws Exception {
        final Async async = context.async();
        UserLocation ul = new UserLocation(UID,38.2,118.33,System.currentTimeMillis());
        JsonObject jo = ul.toJsonObject();
        JsonObject upload = new JsonObject().put("cmd","upload").put("param",jo);
        System.out.println("asking command:"+upload.toString());
        eb.send("Server:LocationManager",upload.toString(), reply->{
            if (reply.succeeded()) {
                System.out.println("result:"+reply.result().body().toString());
            } else {
                System.out.println("failed processing cmd:"+reply.result().body().toString());
            }
            async.complete();
        });
    }
    @Test
    public void init() throws Exception {
    }

    private Vertx vertx;
    private EventBus eb;

    private  int port;
    @Before
    public void setUp(TestContext context) throws Exception {
        Config.enableLog();
        Config.enableDebug(true);
        port = Integer.valueOf(Config.getInstance().getConfig().get("http.port").toString());
        vertx = Vertx.vertx();
        eb = vertx.eventBus();
        DeploymentOptions options = new DeploymentOptions()
                .setConfig(new JsonObject().put("debug", 1)
                );
        vertx.deployVerticle(LocationManager.class.getName(), options,
                context.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext context) throws Exception {
        vertx.close(context.asyncAssertSuccess());
    }
}