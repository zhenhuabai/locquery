package weatherutil;

import common.Config;
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
        JSONObject jo = Config.getInstance().getWeatherConfig();
        String key = jo.get("key").toString().trim();
        if(key == null||key.isEmpty()) {
            logger.error("Weather source will not funtion!");
            queryTemplate = "https://api.thinkpage.cn/v3/weather/now.json?key=tefev55ybfpuqzh3&location=[&language=zh-Hans&unit=c";
        }else{
            logger.error("Weather source key:"+key);
            queryTemplate = "https://api.thinkpage.cn/v3/weather/now.json?key="
            +key+"&location=[&language=zh-Hans&unit=c";
        }
    }
    public XinZhiTianQi(String queryTemplate){
        this.queryTemplate = queryTemplate;
    }
    @Override
    public boolean isAvailable() {
        return !busy;
    }

    @Override
    public void getData(double lon, double lat, Handler<HttpClientResponse> handler) {
        busy = true;
    }

    @Override
    public void getData(String cityname, Handler<HttpClientResponse> handler) {
        if(!busy) {
            busy = true;
            String validCityname = validateCityName(cityname);
            logger.debug("handling:"+validCityname);
            if (validCityname != null && !validCityname.isEmpty()) {
                String url = queryTemplate.replace("[", validCityname);
                try {
                    URI uri = new URI(url.replaceAll(" ","%20"));
                    logger.debug("url="+uri);
                    String scheme = uri.getScheme();
                    String host = uri.getHost();
                    int port = uri.getPort();
                    String path = uri.getPath();
                    String query = uri.getQuery();
                    HttpClientOptions httpOptions = new HttpClientOptions();
                    if (scheme.equalsIgnoreCase("https")) {
                        if (port == -1) {
                            port = 443;
                        }
                        httpOptions.setSsl(true).setVerifyHost(false).setTrustAll(true);
                        String pathstr = path+"?"+query;
                        vertx.createHttpClient(httpOptions).getNow(port,
                                host, pathstr.replaceAll(" ","%20"), handler);
                    } else if (scheme.equalsIgnoreCase("http")) {
                        logger.error("Should NOT happen");
                        handler.handle(null);
                    }
                } catch (Exception e) {
                    logger.error("Wrong Template!Should Not Happen!" + url);
                    handler.handle(null);
                }
            }
        }else{
            logger.warn("Busy! not submitted");
            handler.handle(null);
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
            if(ja != null) {
                JSONObject location = (JSONObject) ((JSONObject) ja.get(0)).get("location");
                JSONObject now = (JSONObject) ((JSONObject) ja.get(0)).get("now");
                //only the first
                String update = ((JSONObject) ja.get(0)).get("last_update").toString();
                String lname = location.get("name").toString();
                String weather = now.get("text").toString();
                String temp = now.get("temperature").toString();
                String path = location.get("path").toString();
                String pathname = lname;
                String [] dir = path.split(",");
                switch (dir.length){
                    case 4:
                        pathname = dir[2]+","+dir[1];
                        break;
                    case 3:
                        pathname = dir[2]+","+dir[1];
                        break;
                    default:
                        break;
                }
                wd = new WeatherData();
                wd.data.put("name", lname);
                //wd.data.put("path", pathname);
                wd.data.put("temperature", temp);
                wd.data.put("weather", weather);
                wd.data.put("update", update);
            }else{
                logger.error("No result returned:"+result);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        vertx.setTimer(10000, id->{busy = false;});
        return wd;
    }
}
