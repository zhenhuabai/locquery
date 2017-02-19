package locutil;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.HashMap;

import static io.vertx.core.json.Json.mapper;

/**
 * Created by 白振华 on 2017/2/16.
 * an object represents the geolocation of a user
 */
public class UserLocation {
    private static final Logger logger = LogManager.getLogger(UserLocation.class);
    public long userid;
    public double lon;
    public double lat;
    public long timestamp;
    private JsonObject cityinfo;
    HashMap<String, Object> mapper = new HashMap<>();
    private UserLocation(JsonObject param){
        long uid = param.getLong("uid");
        this.userid = uid;
        double tmp = param.getDouble("lon");
        this.lon = tmp;
        tmp = param.getDouble("lat");
        this.lat = tmp;
        long timestamp = param.getLong("timestamp");
        this.timestamp = timestamp;
    }
    public UserLocation(long uid, double lon, double lat, long timestamp){
        this.userid = uid;
        this.lon = lon;
        this.lat = lat;
        this.timestamp = timestamp;
    }
    public JsonObject toJsonObject(){
        mapper.clear();
        mapper.put("uid",userid);
        mapper.put("lon",lon);
        mapper.put("lat",lat);
        mapper.put("timestamp",timestamp);
        JsonObject jo = new JsonObject(mapper);
        if(cityinfo != null){
            jo.put("cityinfo",cityinfo);
        }
        return jo;
    }
    @Override
    public String toString(){
        JsonObject jo = toJsonObject();
        return jo.toString();
    }
    public static UserLocation parseUserLocation(JsonObject jo){
        UserLocation ul = null;
        try {
            Long uid = jo.getLong("uid");
            Long timestamp = jo.getLong("timestamp");
            Double lon = jo.getDouble("lon");
            Double lat = jo.getDouble("lat");
            if(uid != null && timestamp != null &&
                    lon != null && lat != null){
                ul = new UserLocation(uid, lon, lat, timestamp);
            }
        }catch (Exception e){
            logger.error("wrong object parameter");
        }
        return ul;
    }
    public void setCityInfo(JsonObject city){
        this.cityinfo = city;
    }
}
