package com.huleibo;

import common.Config;
import io.vertx.core.DeploymentOptions;
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

import static org.junit.Assert.*;

/**
 * Created by 白振华 on 2017/1/19.
 */
@RunWith(VertxUnitRunner.class)
public class CityWeatherServerTest {
    private Vertx vertx;

    private  int port;
    @Before
    public void setUp(TestContext context) throws Exception {
        Config.enableLog();
        port = Integer.valueOf(Config.getInstance().getConfig().get("http.port").toString());
        vertx = Vertx.vertx();
        DeploymentOptions options = new DeploymentOptions()
                .setConfig(new JsonObject().put("debug", 1)
                );
        vertx.deployVerticle(CountryMapServer.class.getName(), options,
                context.asyncAssertSuccess());
        vertx.deployVerticle(CityWeatherServer.class.getName(),
                context.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext context) throws Exception {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void start(TestContext context) throws Exception {
        final Async async = context.async();
        EventBus eb = vertx.eventBus();
        vertx.setTimer(1000, v ->{
            eb.send("Server:Weather", "http://localhost/api/weather?location=109.595,34.645", reply -> {
                if (reply.succeeded()) {
                    JSONParser jp = new JSONParser();
                    try {
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
    }

}