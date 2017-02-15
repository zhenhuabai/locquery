package com.huleibo;

import common.Config;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
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
    @Test
    public void init() throws Exception {
        Thread.sleep(2000);
    }

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
        vertx.deployVerticle(LocationManager.class.getName(), options,
                context.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext context) throws Exception {
        vertx.close(context.asyncAssertSuccess());
    }
}