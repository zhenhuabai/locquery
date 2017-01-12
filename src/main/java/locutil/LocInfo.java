package locutil;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private String[] adms = {"country", "province", "city", "county"};
    private String[] values;
    public LocInfo(String[] storeLoc){
        values = storeLoc;
        for (int i = 0; i < storeLoc.length && i < adms.length; i++) {
            data.put(adms[i], storeLoc[i]);
        }
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
}
