package weatherutil;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by 白振华 on 2017/1/14.
 */
public class WeatherData {
    public final String[] content = {"weather", "temp", "last_update"};
    Map<String, String> data = new HashMap<String,String>();
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
}
