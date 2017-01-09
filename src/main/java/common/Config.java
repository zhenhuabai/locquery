package common;

import io.vertx.core.json.JsonObject;

import java.io.FileReader;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
/**
 * Created by 白振华 on 2017/1/9.
 */
public class Config {
    private static Config ourInstance = new Config();

    public static Config getInstance() {
        return ourInstance;
    }

    private JSONObject config;
    private Config() {
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
}
