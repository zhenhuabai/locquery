package common;

import com.huleibo.LocationManager;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import locutil.UserLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

/**
 * Created by 白振华 on 2017/2/16.
 * Globle MangoDB utility
 */
public class MongoDbHelper {
    private static final Logger logger = LogManager.getLogger(MongoDbHelper.class);
    private static MongoDbHelper ourInstance = new MongoDbHelper();

    public static MongoDbHelper getInstance() {
        return ourInstance;
    }

    private static final String DBKEY = "database";
    private static final String DBURLKEY = "mongodburl";
    public static final String COLLECTION_USERLOCATION = "userlocations";
    private String dbName = null;
    private String dbUrl = null;
    private JSONObject joc = null;
    //TODO investigation! why the static below causes joc init in constructor failure,
    //joc is still null in request!
    //private static JSONObject joc = null;

    private MongoDbHelper() {
        joc = Config.getInstance().getLocationManagerConfig();
    }
    public MongoClient requestClient(Vertx vertx){
        return requestClient(vertx, null);
    }

    public MongoClient requestClient(Vertx vertx, String source){
        //joc = Config.getInstance().getLocationManagerConfig();
        MongoClient client = null;
        if(joc.containsKey(DBKEY)){
            dbName = joc.get(DBKEY).toString();
        }
        if(joc.containsKey(DBURLKEY)){
            dbUrl = joc.get(DBURLKEY).toString();
        }
        if(dbName == null || dbName.isEmpty() ||
                dbUrl == null || dbUrl.isEmpty()){
            logger.error("Database not configured properly");
        } else {
            JsonObject config = new JsonObject();
            config.put("db_name", dbName);
            config.put("connection_string", dbUrl);
            if(source == null || source.isEmpty()) {
                client = MongoClient.createShared(vertx, config);
            } else {
                client = MongoClient.createShared(vertx, config, source);
            }
        }
        return client;
    }
    public static void putUserLocation(MongoClient client, UserLocation uloc,
                                       Handler<AsyncResult<String>>resultHandler){
        logger.debug("putting user location:"+uloc.toString());
        client.insert(COLLECTION_USERLOCATION, uloc.toJsonObject(), res->{
            if (res.succeeded()) {
                String id = res.result();
                logger.debug("Saved: " + uloc.toString());
                resultHandler.handle(res);
            } else {
                logger.error("problem saving:"+uloc.toString());
                res.cause().printStackTrace();
            }
        });
    }
}
