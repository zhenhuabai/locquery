package locutil;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Created by 白振华 on 2017/2/19.
 * This class represents the geopoint structure in mongodb
 */
public class GeoPoint extends JsonObject{
    public final static String TYPE = "type";
    public final static String COODINATES = "coodinates";
    public final static String POINT = "Point";
    public GeoPoint(double longitude, double latitude){
        put(TYPE, POINT);
        JsonArray ja = new JsonArray();
        ja.add(longitude).add(latitude);
        put(COODINATES,ja);
    }
}
