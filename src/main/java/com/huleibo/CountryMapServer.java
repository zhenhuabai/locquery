package com.huleibo;

import common.Config;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import locutil.LocInfo;
import io.vertx.core.AbstractVerticle;
import locutil.GlobeDataStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Map;
//import java.util.logging.Logger;

/**
 * Created by 白振华 on 2017/1/11.
 */
public class CountryMapServer extends AbstractVerticle{
    private int port;
    private String mappath;
    public static boolean inDebug = false;
    private EventBus eb;
    private static final Logger logger = LogManager.getLogger(CountryMapServer.class);
    //Logger Log = Logger.getLogger(this.getClass().getName());
    static public void main(String[] args){
        Vertx vertxx = Vertx.vertx();
        vertxx.deployVerticle(CountryMapServer.class.getName());
    }
    private void setupEnv(){
        int dbg = config().getInteger("debug",0);
        if(dbg == 1) {
            JSONArray maps = Config.getInstance().getMapConfig();
            for (int i = 0; i < maps.size(); i++) {
                JSONObject map = (JSONObject) maps.get(i);
                String name = map.get("name").toString();
                String path1 = map.get("outline").toString();
                port = 18080;
                mappath = "China";
            }
        }else{
            Map<String, String> env = System.getenv();
            try {
                mappath = env.get("map");
                if (mappath == null || mappath.isEmpty()) {
                    logger.error("No map, exit");
                    System.exit(1001);
                }
            }catch (Exception e){
                logger.error("Error getting params");
                System.exit(3);
            }
        }
        logger.info("Map["+mappath+"] will be serviced");
    }
    @Override
    public void start() throws Exception {
        setupEnv();
        eb = vertx.eventBus();
        eb.consumer("Server:"+mappath, message -> {
            String[] loc = message.body().toString().split(",");
            logger.info(String.format("[%s, %s]", loc[0], loc[1]));
            double lat, lon;
            try {
                lat = new Double(loc[0]);
                lon = new Double(loc[1]);
                LocInfo li = GlobeDataStore.getInstance(mappath).findCityDirect(lat, lon);
                if (li != null) {
                    logger.info(String.format("[%f, %f]->%s", lon, lat, li.toString()));
                    message.reply(li.toString());
                } else {
                    message.reply("{}");
                }
            }catch (Exception e){
                e.printStackTrace();
                message.reply("{}");
            }
        });
    }
}
