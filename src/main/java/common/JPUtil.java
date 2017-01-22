package common;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Created by 白振华 on 2017/1/19.
 */
public final class JPUtil {
    static JSONParser jp = new JSONParser();
    public static JSONObject getJOfromString(String s){
        JSONObject jo = null;
        try {
            jo = (JSONObject)jp.parse(s);
        }catch (Exception e){
        }
        return jo;
    }
}
