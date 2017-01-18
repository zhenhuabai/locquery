package weatherutil;

import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.Recycler;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.net.URI;

/**
 * Created by 白振华 on 2017/1/18.
 */
public final class XinZhiTianQi extends WeatherSource {

    private static final Logger logger = LogManager.getLogger(XinZhiTianQi.class);
    private  boolean busy = false;

    public XinZhiTianQi(){
        queryTemplate="https://api.thinkpage.cn/v3/weather/now.json?key=tefev55ybfpuqzh3&location=[&language=zh-Hans&unit=c";
    }
    public XinZhiTianQi(String queryTemplate){
        this.queryTemplate = queryTemplate;
    }
    @Override
    public boolean isAvailble() {
        return !busy;
    }

    @Override
    public void getData(double lon, double lat, Handler<HttpClientResponse> handler) {
        busy = true;
    }

    @Override
    public void getData(String cityname, Handler<HttpClientResponse> handler) {
        busy = true;
        String validCityname = validateCityName(cityname);
        if(validCityname != null){
            String url = queryTemplate.replace("[",validCityname);
            try {
                URI uri = new URI(url);
                logger.info("Query by:" + url);
                String scheme = uri.getScheme();
                String host = uri.getHost();
                int port = uri.getPort();
                String path = uri.getPath();
                String query = uri.getQuery();
                HttpClientOptions httpOptions = new HttpClientOptions();
                if (scheme.equalsIgnoreCase("https")) {
                    if(port == -1) {
                        port = 443;
                    }
                    logger.info(String.format("%s,%s,%d,%s,%s",scheme, host,port,path,query));
                    httpOptions.setSsl(true).setVerifyHost(false).setTrustAll(true);
                    //vertx.createHttpClient(httpOptions).getAbs(url,handler);
                    vertx.createHttpClient(httpOptions).getNow(port,
                            host,path+"?"+query, handler);
                } else if(scheme.equalsIgnoreCase("http")){
                    logger.error("Should NOT happen");
                    handler.handle(null);
                }
            }catch (Exception e){
                logger.error("Wrong Template!Should Not Happen!"+url);
                handler.handle(null);
            }
        }
    }

    private String validateCityName(String cityName){
        return cityName;
    }
    public WeatherData getWeatherData(String result){
        WeatherData wd = null;
        JSONParser jp = new JSONParser();
        logger.info("Data:"+result);
        try {
            JSONObject jo = (JSONObject) jp.parse(result);
            JSONArray ja = (JSONArray)jo.get("results");
            JSONObject location = (JSONObject)((JSONObject)ja.get(0)).get("location");
            JSONObject now = (JSONObject)((JSONObject)ja.get(0)).get("now");
            String update = ((JSONObject)ja.get(0)).get("last_update").toString();
            String lname = location.get("name").toString();
            String weather = now.get("text").toString();
            String temp = now.get("temperature").toString();
            wd = new WeatherData();
            wd.data.put("name",lname);
            wd.data.put("temperature",temp);
            wd.data.put("weather",weather);
            wd.data.put("update",update);
        }catch (Exception e){
            e.printStackTrace();
        }
        vertx.setTimer(10000, id->{busy = false;});
        return wd;
    }
}
