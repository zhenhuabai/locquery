package locsvc;

import locutil.LocInfo;
import locutil.GlobeDataStore;

import java.util.logging.Logger;

/**
 * Created by 白振华 on 2017/1/7.
 */
public class CityQuery {
    public static String[] find(double lat, double lon) throws Exception{
        String[] result = null;
        LocInfo li = GlobeDataStore.getInstance().findCityDirect(lat, lon);
        if(li != null){
            result = li.values();
        }
        return result;
    }
}
