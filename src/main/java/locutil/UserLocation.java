package locutil;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

import java.util.Date;
import java.util.HashMap;

import static io.vertx.core.json.Json.mapper;

/**
 * Created by 白振华 on 2017/2/16.
 * an object represents the geolocation of a user
 */
public class UserLocation {
    public long userid;
    public double lon;
    public double lat;
    public long timestamp;
    HashMap<String, Object> mapper = new HashMap<>();
    public UserLocation(long uid, double lon, double lat, long timestamp){
        this.userid = uid;
        this.lon = lon;
        this.lat = lat;
        this.timestamp = timestamp;
    }
    public JsonObject toJsonObject(){
        mapper.clear();
        mapper.put("userid",userid);
        mapper.put("lon",lon);
        mapper.put("lat",lat);
        mapper.put("timestamp",timestamp);
        JsonObject jo = new JsonObject(mapper);
        return jo;
    }
    @Override
    public String toString(){
        JsonObject jo = toJsonObject();
        return jo.toString();
    }
}
