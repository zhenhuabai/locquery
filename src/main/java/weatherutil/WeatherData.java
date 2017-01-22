package weatherutil;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by 白振华 on 2017/1/14.
 */
public class WeatherData {
    public final static String NAME = "name";
    public final static String WEATHER = "weather";
    public final static String TEMP = "temperature";
    public final static String LAST_UPDATE = "update";
    public final static String LAST = "last";//refresher
    public final String[] content = {"name", "weather", "temperature", "last_update"};
    public Map<String, String> data = new HashMap<String,String>();
    public String toString(){
        StringWriter out = new StringWriter();
        ObjectMapper om = new ObjectMapper();
        HashMap<String,String> outData = new HashMap<>();
        if(data.size()<1){
            outData.put("status","noresult");
        }else{
            data.forEach((key,val)->{
                if (!key.equalsIgnoreCase("last")) {
                    outData.put(key,val);
                }
            });
        }
        try {
            om.writeValue(out, outData);
        }catch (Exception e){
            e.printStackTrace();
        }
        return out.toString();
    }
}
