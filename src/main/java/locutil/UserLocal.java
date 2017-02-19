package locutil;

import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;

/**
 * Created by 白振华 on 2017/2/19.
 * This class represents the local location of a user in database
 */
public class UserLocal {
    private static final Logger logger = LogManager.getLogger(UserLocal.class);
    public static final String UID = "uid";
    public static final String ANALYZERALLOWED = "analyzerAllowed";
    public static final String PROBABILITY = "probability";
    public static final String LANG = "lang";
    public static final String LOCALCITYINFO = "localcityinfo";
    public static final String PROVINCE = "province";
    public static final String LOCAL = "local";
    public static final String CITY = "city";
    public long userid;
    private boolean analyzerAllowed;
    private double probability;
    private String currentLang;
    private JsonObject localCityinfo;
    HashMap<String, Object> mapper = new HashMap<>();
    private UserLocal(JsonObject param){
        userid = param.getLong(UID);
        analyzerAllowed = param.getBoolean(ANALYZERALLOWED);
        probability = param.getDouble(PROBABILITY);
        currentLang = param.getString(LANG);
        localCityinfo = param.getJsonObject(LOCAL);
    }
    private UserLocal(long uid, boolean analyzerAllowed, double probability, String lang,
                     JsonObject localCityinfo){
        this.userid = uid;
        this.analyzerAllowed = analyzerAllowed;
        this.probability = probability;
        this.currentLang = lang;
        this.localCityinfo = localCityinfo;
    }
    //this for saving to db
    public JsonObject toJsonObject(){
        mapper.clear();
        mapper.put(UID,userid);
        mapper.put(ANALYZERALLOWED,analyzerAllowed);
        mapper.put(PROBABILITY,probability);
        mapper.put(LANG,currentLang);
        JsonObject jo = new JsonObject(mapper);
        if(localCityinfo != null){
            jo.put(LOCALCITYINFO,localCityinfo);
        }
        return jo;
    }
    @Override
    public String toString(){
        JsonObject jo = toJsonObject();
        return jo.toString();
    }
    //the parse will validate the data
    public static UserLocal parseUserLocal(JsonObject jo){
        UserLocal ul = null;
        try {
            Boolean analyzerAllowed = jo.getBoolean(ANALYZERALLOWED);
            Double probability = jo.getDouble(PROBABILITY);
            if(probability<0 || probability >1){
                throw new Exception("wrong range");
            }
            String currentLang = jo.getString(LANG);
            JsonObject localCityinfo = jo.getJsonObject(LOCAL);
            String province = localCityinfo.getString(PROVINCE);
            String city = localCityinfo.getString(CITY);
            Long uid = jo.getLong(UID);
            if(uid != null &&  analyzerAllowed!= null &&
                    probability != null && localCityinfo != null &&
                    currentLang != null && probability != null && city != null){
                ul = new UserLocal(uid, analyzerAllowed, probability, currentLang,
                        localCityinfo);
            }
        }catch (Exception e){
            e.printStackTrace();
            logger.error("wrong object parameter");
        }
        return ul;
    }
    public void setLocalCityinfo(JsonObject city){
        this.localCityinfo = city;
    }
}
