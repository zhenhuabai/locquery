package common;

import io.vertx.core.json.JsonObject;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Set;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
/**
 * Created by 白振华 on 2017/1/9.
 */
public class Config{

    private static final Logger logger = LogManager.getLogger(Config.class);
    private static Config ourInstance = new Config();

    public static Config getInstance() {
        return ourInstance;
    }

    private static final String CONFIGFILE = "./config.json";
    private JSONObject config;
    private Config() {
        getConfig();
    }
    public void setConfig(JSONObject jo){
        config = jo;
    }
    public JSONObject getConfig(){
        if (config == null) {
            JSONParser parser = new JSONParser();
            try {

                Reader in = new InputStreamReader(new FileInputStream(CONFIGFILE),"UTF-8");
                config = (JSONObject)parser.parse(in);
                logger.info("Result:"+config.toJSONString());
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        return config;
    }
    public JSONArray getMapConfig(){
        JSONObject jo = new JSONObject();
        JSONArray jsa = null;
        JSONArray jsar = new JSONArray();
        if (config != null){
            jsa = (JSONArray)((JSONArray)config.get("maps")).clone();
            String sys = null;
            switch(OsCheck.getOperatingSystemType() ) {
                case MacOS:
                    sys = "mac";
                    break;
                case Linux:
                    sys = "linux";
                    break;
                case Windows:
                    sys = "windows";
                    break;
                default:
                    sys = "windows";
                    break;
            }
            for (int i = 0; i < jsa.size(); i++) {
                jo.clear();
                Set<String> keys= ((JSONObject)((JSONObject)jsa.get(i))).keySet();
                jo.put("lname", ((JSONObject) jsa.get(i)).get("lname"));
                jo.put("detail", ((JSONObject) (((JSONObject) jsa.get(i)).get("detail"))).get(sys).toString());
                jo.put("columns",((JSONObject)jsa.get(i)).get("columns"));
                jo.put("name",((JSONObject)jsa.get(i)).get("name"));
                if(keys.contains("outline")) {
                    jo.put("outline", ((JSONObject) (((JSONObject) jsa.get(i)).get("outline"))).get(sys).toString());
                }
                if(keys.contains("translation")) {
                    jo.put("translation", ((JSONObject) (((JSONObject) jsa.get(i)).get("translation"))).get(sys).toString());
                }
                jsar.add(jo.clone());
            }
        }
        logger.info("Result:"+jsar.toJSONString());
        return jsar;
    }
    public static void enableLog(){
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        loggerConfig.setLevel(Level.ALL);
        ctx.updateLoggers();
    }
}
