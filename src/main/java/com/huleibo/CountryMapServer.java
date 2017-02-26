package com.huleibo;

import common.Config;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import locutil.LocInfo;
import io.vertx.core.AbstractVerticle;
import locutil.GlobeDataStore;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
//import java.util.logging.Logger;

/**
 * Created by 白振华 on 2017/1/11.
 * This server convert [lat,lon] to city
 * return String converted by json object
 * Empty{} will return if there is no such a city
 */
public class CountryMapServer extends LocApp{
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
    private int port;
    private static String mappath;
    public static boolean inDebug = false;
    private static String mapLname;
    private static boolean translationLoaded = false;
    private static Map<String,String> translation = new HashMap<String, String>();
    private static final Logger logger = LogManager.getLogger(CountryMapServer.class);
    //Logger Log = Logger.getLogger(this.getClass().getName());
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
        vertx.executeBlocking(future -> {
            loadTranslation();
            installSignal();
            future.complete();
        }, res->{
            logger.info("Translation loading completed.");
        });
        eb = vertx.eventBus();
        eb.consumer("Server:"+mappath, message -> {
            String[] loc = message.body().toString().split(",");
            double lat, lon;
            String lang;
            try {
                logger.info(String.format("[%s, %s]", loc[0], loc[1]));
                lat = new Double(loc[0]);
                lon = new Double(loc[1]);
                LocInfo li = GlobeDataStore.getInstance(mappath).findCityDirect(lat, lon);
                if (li != null) {
                    logger.info(String.format("[%f, %f]->%s", lon, lat, li.toString()));
                    //this message from location manager which expects all languages
                    if(loc.length > 2 && loc[2].equalsIgnoreCase("lm")){
                        translateLocInfoCN(li);
                        message.reply(li.toAllString());
                    }else if(loc.length > 2 && loc[2].equalsIgnoreCase("zh")){
                        if(translateLocInfoCN(li)){
                            message.reply(li.toLocalString());
                        } else {
                            message.reply(li.toString());
                        }
                    } else {
                        message.reply(li.toString());
                    }

                } else {
                    message.reply("{}");
                }
            }catch (Exception e){
                e.printStackTrace();
                message.reply("{}");
            }
        });
    }
    private void loadTranslation(){
        if(translation.size()>1){
            logger.info("Translation was loaded for:"+mappath);
            return;
        }else{
            logger.info("Loading translation for:"+mappath);
        }
        JSONArray maps = Config.getInstance().getMapConfig();
        String filename = null;
        for (int i = 0; i < maps.size(); i++) {
            JSONObject map = (JSONObject) maps.get(i);
            String name = map.get("name").toString();
            mapLname = map.get("lname").toString();
            if (name.equals(mappath)) {
                filename = map.get("translation").toString();
                break;
            }
        }
        if(filename != null && !filename.isEmpty()) {
            try {
                Reader in = new InputStreamReader(new FileInputStream(filename),"UTF-8");
                Iterable<CSVRecord> records = CSVFormat.EXCEL
                        .withHeader("enprovince","encity","encounty",
                        "cnprovince","cncity","cncounty").parse(in);
                logger.info("loading translation from:" + filename);
                for (CSVRecord record : records) {
                    if(record.size()<6){
                        logger.warn("Invalid line:"+record.toString());
                        continue;
                    }
                    String enstate = record.get("enprovince");
                    String encity = record.get("encity");
                    String encounty = record.get("encounty");
                    String cnstate = record.get("cnprovince");
                    String cncity = record.get("cncity");
                    String cncounty = record.get("cncounty");
                    if (cnstate != null && !cnstate.isEmpty()
                            && cncity != null && !cncity.isEmpty()
                            && cncounty != null && !cncounty.isEmpty()) {
                        String key = new StringBuffer().append(enstate).append(",")
                                .append(encity).append(",").append(encounty).toString();
                        String value = new StringBuffer().append(cnstate).append(",")
                                .append(cncity).append(",").append(cncounty).toString();
                        translation.put(key.trim().toLowerCase(), value);
                        translationLoaded = true;//make sure when at least one
                        logger.debug(key + "->" + value);
                    } else {
                        logger.warn("Empty value");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Translation not loaded from " + filename);
            }
        }else{
            logger.error("Translation not set for:" + filename);
        }
    }
    private boolean translateLocInfoCN(LocInfo li){
        boolean translated = false;
        if(translationLoaded == false){
            logger.warn("Translation not loaded, no translation!");
            return false;
        }
        String key = li.data.get(li.adms[1]) + "," +
                li.data.get(li.adms[2])+","+ li.data.get(li.adms[3]);
        String skey = key.toLowerCase();
        String value = translation.get(skey);
        if(value != null){
            logger.debug(key+"->"+value);
            String[] cnValues = value.split(",");
            if(cnValues != null && cnValues.length == 3) {
                li.setTranslation(new String[]{mapLname,
                        cnValues[0], cnValues[1], cnValues[2]});
                translated = true;
                li.putExtraTranslation("lang","zh");
            }else{
                logger.error("Wrong dictionay:"+key+"->"+value);
            }
        }else{
            logger.info(skey+" not found, no translation");
        }
        return translated;
    }
    public void stopServer(){
        eb.close(handler->{
            logger.debug("stopped Map Server");
        });
    }
}
