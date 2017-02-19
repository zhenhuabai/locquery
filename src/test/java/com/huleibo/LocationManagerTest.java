package com.huleibo;

import common.Config;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import locutil.UserLocal;
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
        Future<Void>test1 = Future.future();
        Future<Void>test2 = Future.future();
        Future<Void>test3 = Future.future();
        UserLocation ul = new UserLocation(UID,34.797,110.012,System.currentTimeMillis());
        JsonObject jo = ul.toJsonObject();
        JsonObject upload = new JsonObject().put("cmd","upload").put("param",jo);
        System.out.println("asking command:"+upload.toString());
        eb.send("Server:LocationManager",upload.toString(), reply->{
            if (reply.succeeded()) {
                System.out.println("result:"+reply.result().body().toString());
            } else {
                System.out.println("failed processing cmd:"+reply.result().body().toString());
            }
            test1.complete();
        });

        ul = new UserLocation(UID,34.797,118.012,System.currentTimeMillis());
        jo = ul.toJsonObject();
        upload = new JsonObject().put("cmd","upload").put("param",jo);
        System.out.println("asking command:"+upload.toString());
        eb.send("Server:LocationManager",upload.toString(), reply->{
            if (reply.succeeded()) {
                System.out.println("result:"+reply.result().body().toString());
            } else {
                System.out.println("failed processing cmd:"+reply.result().body().toString());
            }
            test2.complete();
        });

        JsonObject ulJO = new JsonObject();
        JsonObject ulcity = new JsonObject();
        ulcity.put(UserLocal.PROVINCE, "Jiangsu");
        ulcity.put(UserLocal.CITY, "Nanjing");
        ulJO.put(UserLocal.UID, UID);
        ulJO.put(UserLocal.ANALYZERALLOWED, true);
        ulJO.put(UserLocal.LANG, "en");
        ulJO.put(UserLocal.PROBABILITY, 0.8);
        ulJO.put(UserLocal.LOCAL, ulcity);
        upload = new JsonObject().put("cmd","setlocal").put("param",ulJO);
        System.out.println("asking command:"+upload.toString());
        eb.send("Server:LocationManager",upload.toString(), reply->{
            if (reply.succeeded()) {
                System.out.println("result:"+reply.result().body().toString());
            } else {
                System.out.println("failed processing cmd:"+reply.result().body().toString());
            }
            test3.complete();
        });
        CompositeFuture.join(test1,test2,test3).setHandler(ar ->{
            if(ar.succeeded()){
                System.out.println("All tests finished");
            }else{
                System.out.println("Error, some tests not finished");
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
        vertx.deployVerticle(CountryMapServer.class.getName(), options,
                context.asyncAssertSuccess());
        vertx.deployVerticle(LocationManager.class.getName(), options,
                context.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext context) throws Exception {
        //vertx.close(context.asyncAssertSuccess());
    }
}