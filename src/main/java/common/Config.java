package common;

import io.vertx.core.json.JsonObject;

import java.io.FileReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
                config = (JSONObject)parser.parse(new FileReader("./config.json"));
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
                jo.put("outline", ((JSONObject)(((JSONObject)jsa.get(i)).get("outline"))).get(sys).toString());
                jo.put("detail", ((JSONObject)(((JSONObject)jsa.get(i)).get("detail"))).get(sys).toString());
                jo.put("name",((JSONObject)jsa.get(i)).get("name"));
                jo.put("columns",((JSONObject)jsa.get(i)).get("columns"));
                jsar.add(jo.clone());
            }
        }
        logger.info("Result:"+jsar.toJSONString());
        return jsar;
    }
}
