package com.huleibo;

import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by 白振华 on 2017/1/11.
 */
@RunWith(VertxUnitRunner.class)
public class CountryMapServerTest {
    private CountryMapServer cms;
    private int port;
    private Vertx vertx;
    private Map<String, String> envOrg ;

    @Before
    public void setUp(TestContext context) {
        vertx = Vertx.vertx();
        DeploymentOptions options = new DeploymentOptions()
                .setConfig(new JsonObject().put("debug", 1)
                );
        vertx.deployVerticle(CountryMapServer.class.getName(), options,
                context.asyncAssertSuccess());
    }
    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void start() throws Exception {
        Object waiter = new Object();
        boolean finished = false;
        NetClient client= vertx.createNetClient();
        client.connect(18080, "localhost", res -> {
            if (res.succeeded()) {
                System.out.println("Connected!");
                NetSocket socket = res.result();
                System.out.println("Sending 108,34!");
                socket.write("118.83,32.41");
                socket.write("109.6,34.51");
                socket.handler(result->{
                   System.out.println("Got:"+result.toString());
                });
            } else {
                System.out.println("Failed to connect: " + res.cause().getMessage());
            }
        });
        //The wait is to give client time to run, when the start() finishes,
        // the port test case is finished too before the client&server have time
        //chat

        Thread.sleep(10000);
        client.close();
    }

}