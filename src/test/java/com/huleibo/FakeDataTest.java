package com.huleibo;

import common.Config;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import locutil.UserLocal;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by 白振华 on 2017/2/20.
 */
@RunWith(VertxUnitRunner.class)
public class FakeDataTest {
    private long UID = 800001;
    private String[][] locSet = {
            {"China","Jiangsu","Nanjing","Nanjing","118.778074","32.057236"},
            {"China","Shaanxi","Weinan","Dali","110.01195","34.79684"},
            {"China","Jiangsu","Wuxi","Xishan","120.305456","31.570037"},
            {"China","Jiangsu","Changzhou","Wujin","119.981861","31.771397"},
            {"China","Jiangsu","Zhenjiang","Jurong","119.455835","32.204409"},
            {"China","Beijing","Chaoyang","","116.395645","39.929986"},
            {"China","Shanghai","Jingan","","121.454756","31.235381"},
            {"China","Anhui","Hefei","Heifei","117.282699","31.866942"},
            {"China","Anhui","Fuyang","Fuyang","115.820932","32.901211"},
            {"China","Shandong","Qingdao","Qingdao","120.384428","36.105215"}
    };
    //the distribution weight to generate data
    //this example, the chance of Nanjing is 50%
    //private int [] nanjing50 = {9,1,1,1,1,1,1,1,1,1};
    private int [] nanjing80percent = {40,2,1,1,1,1,1,1,1,1};
    private int [] nanjing40weinan40percent = {20,20,3,1,1,1,1,1,1,1};
    private int [] nanjing50weinan20wuxi16percent = {25,10,8,1,1,1,1,1,1,1};
    private JsonArray generateHistory(long uid, String[][]cities, int[]distribution, int recNo, int lastdays){
        long mind = 24*3600*1000;
        long currentMillis = System.currentTimeMillis();
        JsonArray ja = new JsonArray();
        if(cities.length == distribution.length){//must be equal
            int[] uplimit = new int[distribution.length];
            int total = 0;
            for (int i = 0; i<distribution.length; i++) {
                total += distribution[i];
                uplimit[i] = total - 1;
            }
            for (int j = 0; j < recNo; j++){
                double r = Math.random();
                int index = (int)(r * total);
                System.out.printf("%f,%d-%d\n",r,index,total);
                for(int v = 0; v < uplimit.length; v++){
                    if(index <= uplimit[v]) {
                        JsonObject jo = new JsonObject();
                        jo.put("uid",uid);
                        jo.put("lat",Double.parseDouble(cities[v][4]));
                        jo.put("lon",Double.parseDouble(cities[v][5]));
                        double dd = Math.random();
                        int backdays = (int)(dd * lastdays);
                        long thatdaymillis = currentMillis - backdays * mind;
                        jo.put("timestamp",thatdaymillis);
                        String val = jo.encode();
                        ja.add(jo);
                        System.out.print(String.format("No.%d(%s,%s)->%s\n",v,cities[v][1],cities[v][2],val));
                        break;
                    }
                }
            }
        }
        return ja;
    }
    private void postUserLocation(JsonArray locations){
        AtomicInteger result = new AtomicInteger(0);
        int size = locations.size();
        locations.forEach(o ->{
            JsonObject jo = (JsonObject) o;
            String val = jo.encode();
            vertx.createHttpClient().post(port, "localhost", "/api/userlocation",
                    response -> {
                        response.handler(body->{
                            int no = result.incrementAndGet();
                            System.out.println("Result No."+no+"+ is:"+body.toJsonObject());
                        });
                    })
                    .putHeader("Content-Length", val.length() + "")
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .write(val).end();

        });
        //make sure all request handled
        do {
            int received = result.get();
            System.out.println("handled: "+received+" of:"+size);
            if(received == size){
                break;
            }else{
                try {
                    Thread.sleep(1000);
                }catch (Exception e){

                }
            }
        }while (true);
    }
    @Test
    public void testGenerator(){
        generateHistory(100001,locSet,nanjing80percent,10,30);
    }
    @Test
    public void createLocationHistory(TestContext context){
        final Async async = context.async();
        JsonArray hist = generateHistory(100001,locSet,nanjing80percent,100,30);
        postUserLocation(hist);
        hist = generateHistory(100002,locSet,nanjing40weinan40percent,200,30);
        postUserLocation(hist);
        hist = generateHistory(100003,locSet,nanjing50weinan20wuxi16percent,100,30);
        postUserLocation(hist);
        async.complete();
    }
    @Test
    public void setUserLocal(TestContext context) throws Exception {
        Config.getInstance().getLocationManagerConfig();
        final Async async = context.async();

        JsonObject ulJO = new JsonObject();
        JsonObject ulcity = new JsonObject();
        ulcity.put(UserLocal.PROVINCE, "Jiangsu");
        ulcity.put(UserLocal.CITY, "Wuxi");
        ulJO.put(UserLocal.UID, UID);
        ulJO.put(UserLocal.ANALYZERALLOWED, false);
        ulJO.put(UserLocal.LANG, "en");
        ulJO.put(UserLocal.PROBABILITY, 0.8);
        ulJO.put(UserLocal.LOCAL, ulcity);
        String val = ulJO.encode();
        System.out.println("posting:"+val);
        vertx.createHttpClient().put(port, "localhost", "/api/userlocal",
                response -> {
                    response.handler(body -> {
                        context.assertTrue(body.toJsonObject().getString("result").equals("OK"));
                        async.complete();
                    });
                })
                .putHeader("Content-Length", val.length() + "")
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
                        context.assertTrue(body.toJsonObject().getBoolean("result"));
                        async.complete();
                    });
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
    }

    @After
    public void tearDown(TestContext context) {
        vertx.undeploy(CityWeatherServer.class.getName());
        vertx.undeploy(CountryMapServer.class.getName(), handler->{
        });
    }
}
