package common;

import io.vertx.core.json.JsonObject;

import java.io.FileReader;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
/**
 * Created by 白振华 on 2017/1/9.
 */
public class Config extends Loggable{
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
            switch(OsCheck.getOperatingSystemType() ){
               case MacOS:
                   for (int i = 0; i < jsa.size(); i++) {
                       jo.clear();
                       jo.put("outline", ((JSONObject)(((JSONObject)jsa.get(i)).get("outline"))).get("mac").toString());
                       jo.put("detail", ((JSONObject)(((JSONObject)jsa.get(i)).get("detail"))).get("mac").toString());
                       jo.put("name",((JSONObject)jsa.get(i)).get("name"));
                       jsar.add(jo.clone());
                   }
           }
        }
        Log.info("Result:"+jsar.toJSONString());
        return jsar;
    }
}
