package com.huleibo;

import com.fasterxml.jackson.databind.ObjectMapper;
import common.Config;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

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
        Config.enableLog();
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
    public void start(TestContext context) throws Exception {
        final Async async = context.async();
        Object waiter = new Object();
        boolean finished = false;
        EventBus eb = vertx.eventBus();
        vertx.setTimer(1000, v ->{
        //vertx.setPeriodic(1000, v -> {


            eb.send("Server:China", "118.83,32.41", reply -> {
                if (reply.succeeded()) {
                    JSONParser jp = new JSONParser();
                    try {
                        JSONObject jo = (JSONObject)jp.parse(reply.result().body().toString());
                        String cn = jo.get("country").toString();
                        boolean t = cn.matches("China|中国");
                        context.assertTrue(t);
                        //context.assertEquals("China", jo.get("country"));
                        System.out.println("Received reply " + reply.result().body());
                        async.complete();
                    }catch (Exception e){
                        System.out.println("Problem getting data");
                    };
                } else {
                    System.out.println("No reply");
                }
            });

        });
        /*
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

        client.close();
        Thread.sleep(10000);
        */
    }

}