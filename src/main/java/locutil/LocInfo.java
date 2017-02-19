package locutil;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by 白振华 on 2017/1/7.
 */
public class LocInfo {
    private static final Logger logger = LogManager.getLogger(LocInfo.class);
    public Map<String, String> data = new LinkedHashMap<String, String>();
    public Map<String, String> translatedData = null;
    public String[] adms = {"country", "province", "city", "county"};
    private String[] values;
    public LocInfo(String[] storeLoc){
        values = storeLoc;
        for (int i = 0; i < storeLoc.length && i < adms.length; i++) {
            data.put(adms[i], storeLoc[i]);
        }
        //language default as English
        data.put("lang","en");
    }
    public String[] values(){
        return values;
    }
    public String toString(){
        StringWriter out = new StringWriter();
        ObjectMapper om = new ObjectMapper();
        try {
            om.writeValue(out, data);
        }catch (Exception e){
            e.printStackTrace();
        }
        return out.toString();
    }
    public String toLocalString(){
        StringWriter out = new StringWriter();
        if(translatedData != null) {
            ObjectMapper om = new ObjectMapper();
            try {
                om.writeValue(out, translatedData);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return out.toString();
        } else {
            return toString();
        }
    }
    public String toAllString(){
        String en = toString();
        String cn = toLocalString();
        JsonObject all = new JsonObject().put("en",en).put("zh",cn);
        return all.toString();
    }
    public void print(){
        StringWriter out = new StringWriter();
        ObjectMapper om = new ObjectMapper();
        try {
            om.writeValue(out, data);
        }catch (Exception e){
            e.printStackTrace();
        }
        logger.info(out.toString());
    }
    public void setTranslation(String[] tr){
        if(tr.length == adms.length){
            translatedData = new LinkedHashMap<String, String>();
            for (int i = 0; i < adms.length; i++) {
                translatedData.put(adms[i], tr[i]);
            }
        }
    }
    public void putExtraTranslation(String key, String val){
        translatedData.put(key, val);
    }
    public void putExtra(String key, String val){
        data.put(key, val);
    }
}
