package common;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by 白振华 on 2017/1/7.
 */
public class LocInfo {
    public Map<String, String> data = new LinkedHashMap<String, String>();
    public LocInfo(String[] storeLoc){
        data.put("country", storeLoc[0]);
        data.put("province", storeLoc[1]);
        data.put("city", storeLoc[2]);
        data.put("county", storeLoc[3]);
    }
}
