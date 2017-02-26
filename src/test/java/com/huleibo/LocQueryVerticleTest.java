package com.huleibo;

import common.Config;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import locutil.GlobeDataStore;
import locutil.UserLocal;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Created by 白振华 on 2017/1/7.
 */
@RunWith(VertxUnitRunner.class)
public class LocQueryVerticleTest {

    private long UID = 100001;
    private long[] uids = {100001,100002,100003};
    @Test
    public void getIsnonLocal(TestContext context) throws Exception {
        final Async async = context.async();
        //{"China","Jiangsu","Nanjing","Nanjing","118.778074","32.057236"},
        String val = "/api/isnonlocal?uid=100001&location=118.778074,32.057236&probability=0.9";
        System.out.println("getting:"+val);
        vertx.createHttpClient().getNow(port, "localhost", val,
                response -> {
                    response.handler(body -> {
                        System.out.println("result-->:"+body.toString());
                        JsonObject res = body.toJsonObject();
                        context.assertTrue(body.toJsonObject().getString("result") != null);
                        async.complete();
                    });
                });
    }
    @Test
    public void getUserLocal(TestContext context) throws Exception {
        final Async async = context.async();

        String val = "/api/userlocal?uid=100001,100002,100003,100004&lang=en";
        System.out.println("posting:"+val);
        vertx.createHttpClient().getNow(port, "localhost", val,
                response -> {
                    response.handler(body -> {
                        System.out.println("result:"+body.toString());
                        JsonObject res = body.toJsonObject();
                        context.assertTrue(body.toJsonObject().getString("result") != null);
                        async.complete();
                    });
                });
    }

    @Test
    public void getUserLocalErr2(TestContext context) throws Exception {
        final Async async = context.async();

        String val = "/api/userlocal?uid=100001";
        System.out.println("posting:"+val);
        vertx.createHttpClient().getNow(port, "localhost", val,
                response -> {
                    System.out.println("received :"+response.statusCode());
                    context.assertTrue(response.statusCode()==400);
                    context.assertTrue(response.statusMessage().contains("Illegal"));
                    async.complete();
                });
    }
    @Test
    public void getUserLocalErr1(TestContext context) throws Exception {
        final Async async = context.async();

        String val = "/api/userlocal?uid=100001&lang=";
        System.out.println("posting:"+val);
        vertx.createHttpClient().getNow(port, "localhost", val,
                response -> {
                    System.out.println("received :"+response.statusCode());
                    context.assertTrue(response.statusCode()==400);
                    context.assertTrue(response.statusMessage().contains("Illegal"));
                    async.complete();
                });
    }
    @Test
    public void setUserLocalErr1(TestContext context) throws Exception {
        Config.getInstance().getLocationManagerConfig();
        final Async async = context.async();

        JsonObject ulJO = new JsonObject();
        JsonObject ulcity = new JsonObject();
        JsonObject cityinfo = new JsonObject();
        ulcity.put(UserLocal.PROVINCE, "Jiangsu");
        ulcity.put(UserLocal.CITY, "Wuxi");
        cityinfo.put("en",ulcity.copy());
        ulJO.put(UserLocal.UID, 100005);
        ulJO.put(UserLocal.ANALYZERALLOWED, false);
        //ulJO.put(UserLocal.LANG, "en");
        //ulJO.put(UserLocal.PROBABILITY, 0.8);
        ulJO.put(UserLocal.CITYINFO, cityinfo);
        String val = ulJO.encode();
        System.out.println("posting:"+val);
        vertx.createHttpClient().put(port, "localhost", "/api/userlocal",
                response -> {
                    response.handler(body -> {
                        System.out.println("setUserLocal:"+body.toString());
                        context.assertTrue(body.toJsonObject().getString("result").equals("OK"));
                        async.complete();
                    });
                })
                .putHeader("Content-Length", val.getBytes("utf-8").length + "")
                .putHeader("content-type", "application/json; charset=utf-8")
                .write(val).end();
    }
    @Test
    public void setUserLocal(TestContext context) throws Exception {
        Config.getInstance().getLocationManagerConfig();
        final Async async = context.async();

        JsonObject ulJO = new JsonObject();
        JsonObject ulcity = new JsonObject();
        JsonObject cityinfo = new JsonObject();
        ulcity.put(UserLocal.PROVINCE, "Jiangsu");
        ulcity.put(UserLocal.CITY, "Wuxi");
        cityinfo.put("en",ulcity.copy());
        ulcity.clear();
        ulcity.put(UserLocal.PROVINCE, "江苏");
        ulcity.put(UserLocal.CITY, "无锡");
        cityinfo.put("zh",ulcity);
        ulJO.put(UserLocal.UID, uids[0]);
        ulJO.put(UserLocal.ANALYZERALLOWED, false);
        ulJO.put(UserLocal.LANG, "en");
        ulJO.put(UserLocal.PROBABILITY, 0.8);
        ulJO.put(UserLocal.CITYINFO, cityinfo);
        String val = ulJO.encode();
        System.out.println("posting:"+val);
        vertx.createHttpClient().put(port, "localhost", "/api/userlocal",
                response -> {
                    response.handler(body -> {
                        System.out.println("setUserLocal:"+body.toString());
                        context.assertTrue(body.toJsonObject().getString("result").equals("OK"));
                        async.complete();
                    });
                })
                .putHeader("Content-Length", val.getBytes("utf-8").length + "")
                .putHeader("content-type", "application/json; charset=utf-8")
                .write(val).end();
    }
    @Test
    public void uploadUserLocation(TestContext context) throws Exception {
        Config.getInstance().getLocationManagerConfig();
        final Async async = context.async();
        JsonObject jo = new JsonObject();
        jo.put("uid",20001);
        jo.put("lat",38.01);
        jo.put("lon",128.2);
        jo.put("timestamp",System.currentTimeMillis());
        String val = jo.encode();
        System.out.println("posting:"+val);
        vertx.createHttpClient().post(port, "localhost", "/api/userlocation",
                response -> {
                    response.handler(body -> {
                        System.out.println("response 2:"+body.toString());
                        context.assertTrue(body.toJsonObject().getString("result").contains("error"));
                        async.complete();
                    });
                })
                .putHeader("Content-Length", val.length() + "")
                .putHeader("content-type", "application/json; charset=utf-8")
                .write(val).end();
    }
    @Test
    public void uploadUserLocationErr1(TestContext context) throws Exception {
        Config.getInstance().getLocationManagerConfig();
        final Async async = context.async();
        JsonObject jo = new JsonObject();
        jo.put("lat",38.01);
        jo.put("lon",128.2);
        jo.put("timestamp",System.currentTimeMillis());
        String val = jo.encode();
        System.out.println("posting:"+val);
        vertx.createHttpClient().post(port, "localhost", "/api/userlocation",
                response -> {
                    System.out.println("received :"+response.statusCode());
                    context.assertTrue(response.statusCode()==400);
                    context.assertTrue(response.statusMessage().contains("Illegal"));
                    async.complete();
                })
                .putHeader("Content-Length", val.length() + "")
                .putHeader("content-type", "application/json; charset=utf-8")
                .write(val).end();
    }

    private Vertx vertx;
    private int port;

    @Before
    public void setUp(TestContext context) {
        vertx = Vertx.vertx();
        Config.enableLog();
        Config.enableDebug(true);
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
        vertx.deployVerticle(LocationManager.class.getName(),
                context.asyncAssertSuccess());
        /*
        vertx.deployVerticle(CityWeatherServer.class.getName(),
                context.asyncAssertSuccess());
                */
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
        List<Future> waitList = new ArrayList<Future>();
        for(int i = 0; i < 5; i ++){
            waitList.add(i,Future.future());
        }
        vertx.createHttpClient().getNow(port, "localhost", "/api/city?lon=109.594513&lat=34.644989",
                response -> {
                    response.handler(body -> {
                        System.out.print(body.toString());
                        boolean en = body.toString().contains("Weinan");
                        context.assertTrue(en);
                        waitList.get(0).complete();
                    });
                });
        vertx.createHttpClient().getNow(port, "localhost", "/api/city?lon=109.594513&lat=34.644989&lang=zh",
                response -> {
                    response.handler(body -> {
                        System.out.print(body.toString());
                        boolean ch = body.toString().contains("渭南");
                        context.assertTrue(ch);
                        waitList.get(1).complete();
                    });
                });
        vertx.createHttpClient().getNow(port, "localhost", "/api/city?lon=109.594513&lat=34.644989&lang=en",
                response -> {
                    response.handler(body -> {
                        System.out.print(body.toString());
                        boolean en = body.toString().contains("Weinan");
                        context.assertTrue(en);
                        waitList.get(2).complete();
                    });
                });
        vertx.createHttpClient().getNow(port, "localhost", "/api/city?lon=122.715721&lat=52.949659&lang=zh",
                response -> {
                    response.handler(body -> {
                        System.out.print(body.toString());
                        boolean en = body.toString().contains("漠河");
                        context.assertTrue(en);
                        waitList.get(3).complete();
                    });
                });
        vertx.createHttpClient().getNow(port, "localhost", "/api/city?lon=109.594513&lat=34.644989&lang=none",
                response -> {
                    response.handler(body -> {
                        System.out.print(body.toString());
                        boolean ch = body.toString().contains("Weinan");
                        context.assertTrue(ch);
                        waitList.get(4).complete();
                    });
                });
        CompositeFuture.all(waitList).setHandler(cmplete->{
            async.complete();
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

    @Test
    public void testQueryWeather(TestContext context) {
        final Async async = context.async();
        vertx.deployVerticle(CityWeatherServer.class.getName(),
                context.asyncAssertSuccess());
        vertx.createHttpClient().getNow(port, "localhost", "/api/weather?location=xxx,ttt",
                response -> {
                    response.handler(body -> {
                        System.out.print(body.toString());
                        boolean en = body.toString().contains("error");
                        context.assertTrue(en);
                    });
                });
        vertx.createHttpClient().getNow(port, "localhost", "/api/weather?location=",
                response -> {
                    response.handler(body -> {
                        System.out.print(body.toString());
                        boolean en = body.toString().contains("error");
                        context.assertTrue(en);
                    });
                });
        vertx.createHttpClient().getNow(port, "localhost", "/api/weather?location=118.8,32.05",
                response -> {
                    response.handler(body -> {
                        System.out.print(body.toString());
                        boolean en = body.toString().contains("weather");
                        context.assertTrue(en);
                        async.complete();
                    });
                });
        try {
            Thread.sleep(10000);
        }catch (Exception e){}
    }
}