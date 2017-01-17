package com.huleibo;

import common.Config;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import locutil.GlobeDataStore;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.ServerSocket;


/**
 * Created by 白振华 on 2017/1/7.
 */
@RunWith(VertxUnitRunner.class)
public class LocQueryVerticleTest {
    private Vertx vertx;
    private int port;

    @Before
    public void setUp(TestContext context) {
        vertx = Vertx.vertx();
        Config.enableLog();
        port = Integer.valueOf(Config.getInstance().getConfig().get("http.port").toString());
        DeploymentOptions options = new DeploymentOptions()
                .setConfig(new JsonObject().put("http.port", port)
                );
        vertx.deployVerticle(LocQueryVerticle.class.getName(), options,
                context.asyncAssertSuccess());

        options = new DeploymentOptions()
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
    public void testMyApplication(TestContext context) {
        final Async async = context.async();
        vertx.createHttpClient().getNow(port, "localhost", "/",
                response -> {
                    response.handler(body -> {
                        context.assertTrue(body.toString().contains("Hello"));
                        async.complete();
                    });
                });
    }

    @Test
    public void testQueryCity(TestContext context) {
        final Async async = context.async();
        vertx.createHttpClient().getNow(port, "localhost", "/api/city?lon=109.594513&lat=34.644989",
                response -> {
                    response.handler(body -> {
                        System.out.print(body.toString());
                        boolean en = body.toString().contains("Weinan");
                        context.assertTrue(en);
                        async.complete();
                    });
                });
        vertx.createHttpClient().getNow(port, "localhost", "/api/city?lon=109.594513&lat=34.644989&lang=zh",
                response -> {
                    response.handler(body -> {
                        System.out.print(body.toString());
                        boolean ch = body.toString().contains("渭南");
                        context.assertTrue(ch);
                        async.complete();
                    });
                });
        vertx.createHttpClient().getNow(port, "localhost", "/api/city?lon=109.594513&lat=34.644989&lang=en",
                response -> {
                    response.handler(body -> {
                        System.out.print(body.toString());
                        boolean en = body.toString().contains("Weinan");
                        context.assertTrue(en);
                        async.complete();
                    });
                });
        vertx.createHttpClient().getNow(port, "localhost", "/api/city?lon=122.715721&lat=52.949659&lang=zh",
                response -> {
                    response.handler(body -> {
                        System.out.print(body.toString());
                        boolean en = body.toString().contains("Mohe");
                        context.assertTrue(en);
                        async.complete();
                    });
                });
        vertx.createHttpClient().getNow(port, "localhost", "/api/city?lon=109.594513&lat=34.644989&lang=none",
                response -> {
                    response.handler(body -> {
                        System.out.print(body.toString());
                        boolean ch = body.toString().contains("Weinan");
                        context.assertTrue(ch);
                        async.complete();
                    });
                });
    }
    @Test
    public void testQueryCityErr(TestContext context) {
        final Async async = context.async();
        vertx.createHttpClient().getNow(port, "localhost", "/api/city",
                response -> {
                    context.assertTrue(response.statusCode() == 400);
                    async.complete();
                });
    }
    @Test
    public void testQueryEncoding(TestContext context) {
        final Async async = context.async();

        //test characters
        vertx.createHttpClient().getNow(port, "localhost", "/api/city?lon=117.944367&lat=28.40568",
                response -> {
                    response.handler(body -> {
                        System.out.print(body.toString());
                        boolean en = body.toString().contains("Shangrao");
                        context.assertTrue(en);
                        async.complete();
                    });
                });
    }
}