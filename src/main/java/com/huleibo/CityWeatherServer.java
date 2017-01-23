package com.huleibo;

import common.JPUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import jdk.nashorn.internal.parser.JSONParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import sun.misc.Signal;
import sun.misc.SignalHandler;
import weatherutil.WeatherData;
import weatherutil.WeatherDatabase;

import java.net.URI;

/**
 * Created by 白振华 on 2017/1/19.
 */
public class CityWeatherServer extends AbstractVerticle implements SignalHandler{
    private static final Logger logger = LogManager.getLogger(CountryMapServer.class);

    private EventBus eb;
    @Override
    public void start() throws Exception {
        vertx.executeBlocking(future -> {
            initWeatherSources();
            installSignal();
            future.complete();
        }, res->{
            logger.info("Weather sources initialized.");
        });
        eb = vertx.eventBus();
        eb.consumer("Server:Weather", message -> {
            String uris = message.body().toString();
            try {
                URI uri = new URI(uris);
                String query = uri.getQuery();
                String location = null;
                String[] params = query.split("&");
                for (int i = 0; i < params.length; i++) {
                    if(params[i].contains("location")){
                        location = params[i].substring(params[i].indexOf("=")+1);
                        break;
                    }
                }
                if (location == null || location.isEmpty()) {
                    message.reply("{\"error\":\"Wrong params\"}");
                } else {
                    String f = location;
                    if(location.contains(",")) {//lon,lat format
                        eb.send("Server:China", location.trim(), reply -> {
                            if (reply.succeeded()) {
                                logger.info(String.format("[%s]->%s", f, reply.result().body().toString()));
                                String result = reply.result().body().toString();
                                JSONObject jo = JPUtil.getJOfromString(result);
                                WeatherData wd = new WeatherData();
                                try {
                                    if (jo != null && !jo.isEmpty() && jo.containsKey("country")) {
                                        String country = jo.get("country").toString();
                                        String province = jo.get("province").toString();
                                        String city = jo.get("city").toString();
                                        if (country != null && !country.isEmpty()
                                                && province != null && !province.isEmpty()
                                                && city != null && !city.isEmpty()) {
                                            wd = WeatherDatabase.getInstance()
                                                    .getWeatherData(country, province, city, "");
                                            logger.debug(String.format("%s,%s,%s\n",
                                                    country, province, city));
                                        } else {
                                            wd.data.put("error", "city not found");
                                            logger.info("City not found!");
                                        }
                                    }else{
                                        wd.data.put("error", "city not found");
                                    }
                                }catch (Exception e){
                                    wd.data.put("error", "city not found");
                                }
                                message.reply(wd.toString());
                            } else {
                                logger.warn("Server no reply for:" + f);
                                message.reply("Error: Map server not ready!");
                            }
                        });
                    }else {
                        WeatherData wd = WeatherDatabase.getInstance().getWeatherData(location);
                        message.reply(wd.toString());
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
                message.reply("{\"error\":\""+e.getMessage()+"\"}");
            }
        });
    }
    private void initWeatherSources(){
        WeatherDatabase.getInstance();//let's go

    }

    public void handle(Signal signalName) {
        logger.warn("Reveived signal:"+signalName.toString());
        if(signalName.getName().equalsIgnoreCase("term")){
            logger.warn("TERM Reveived! Exiting app");
            if(eb != null) {
                eb.close(handler -> {
                    logger.info("Application closed");
                });
            }
        }
    }
    private void installSignal(){
        Signal.handle(new Signal("TERM"), this);
    }
}
