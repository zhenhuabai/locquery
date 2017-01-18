package weatherutil;

import common.Config;
import io.vertx.core.Context;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Created by 白振华 on 2017/1/18.
 */

@RunWith(VertxUnitRunner.class)
public class XinZhiTianQiTest {
    XinZhiTianQi xz;
    @Before
    public void setUp() {
        Config.enableLog();
       xz = new XinZhiTianQi();
    }
    //@Test
    public void getRepeatedData(TestContext context) throws Exception {
        final Async async = context.async();
        String[] cities = {"tianjin", "beijing"};
        for (int i = 0; i < cities.length; i++) {
            while (!xz.isAvailble()){
                Thread.sleep(1000);
                System.out.println("I am waiting...");
            }
            if(xz.isAvailble()) {
                xz.getData(cities[i], httpClientResponse -> {
                    httpClientResponse.handler(
                            body -> {
                                WeatherData wd = xz.getWeatherData(body.toString());
                                System.out.println(wd.toString());
                            });
                });
            }
        }
    }
    @Test
    public void getData(TestContext context) throws Exception {
        final Async async = context.async();
        xz.getData("Beijing", httpClientResponse -> {
            httpClientResponse.handler(
                    body->{
                        JSONParser jp = new JSONParser();
                        System.out.printf(body.toString());
                        String name = "";
                        try {
                            JSONObject jo = (JSONObject) jp.parse(body.toString());
                            JSONArray ja = (JSONArray)jo.get("results");
                            JSONObject location = (JSONObject)((JSONObject)ja.get(0)).get("location");
                            name = location.get("name").toString();
                            System.out.print("name = "+name);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                        context.assertTrue(name.equals("北京"));
                        async.complete();
                    }
            );
        });
    }

    @Test
    public void getData1() throws Exception {

    }

}