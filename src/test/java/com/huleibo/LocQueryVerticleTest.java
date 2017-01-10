package com.huleibo;

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
        ServerSocket socket = null;
        try{
             socket = new ServerSocket(0);
             port = socket.getLocalPort();
             System.out.println("port availabe is:"+port);
        } catch (Exception e){
        } finally {
            if (socket != null) {
                try{
                    socket.close();
                }catch(Exception ee){}
            };
        }

        port = 8080;
        DeploymentOptions options = new DeploymentOptions()
                .setConfig(new JsonObject().put("http.port", port)
                );
        vertx.deployVerticle(LocQueryVerticle.class.getName(),
                context.asyncAssertSuccess());

        NetClientOptions cloptions = new NetClientOptions().setConnectTimeout(10000);
        NetClient client = vertx.createNetClient(cloptions);
        client.connect(4321, "localhost", res -> {
            if (res.succeeded()) {
                System.out.println("Connected!");
                NetSocket clsocket = res.result();

                clsocket.handler(sck->{
                    System.out.println(sck.toString());
                });
                clsocket.write("lat=1,lon=2");

            } else {
                System.out.println("Failed to connect: " + res.cause().getMessage());
            }
});
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
        vertx.createHttpClient().getNow(port, "localhost", "/api/city?lat=109.594513&lon=34.644989",
                response -> {
                    response.handler(body -> {
                        context.assertTrue(body.toString().contains("Weinan"));
                        async.complete();
                    });
                });
    }
    @Test
    public void testQueryCityErr(TestContext context) {
        final Async async = context.async();
        vertx.createHttpClient().getNow(port, "localhost", "/api/city",
                response -> {
                    response.handler(body -> {
                        context.assertTrue(body.toString().contains("Error"));
                        async.complete();
                    });
                });
    }
}