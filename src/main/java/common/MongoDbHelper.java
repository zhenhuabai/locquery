package common;

import com.huleibo.LocationManager;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import locutil.UserLocal;
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
    public static final String TESTDATABASE = "db4buildandtest";
    private static final String DBGDBKEY = "dbgdatabase";
    private static final String DBURLKEY = "mongodburl";
    public static final String COLLECTION_USERLOCATION = "userlocations";
    public static final String COLLECTION_USERLOCAL = "userlocal";
    public static final String COLLECTION_USERLOCALANALYZED = "userlocalanalyzed";
    private String dbName = null;
    private String dbUrl = null;
    private JSONObject joc = null;

    private MongoDbHelper() {
        joc = Config.getInstance().getLocationManagerConfig();
    }
    public MongoClient requestClient(Vertx vertx){
        return requestClient(vertx, null);
    }

    public MongoClient requestClient(Vertx vertx, String source){
        MongoClient client = null;
        if (joc.containsKey(DBKEY)) {
            dbName = joc.get(DBKEY).toString();
            if(Config.isDebug()) {
                dbName = TESTDATABASE;
            }
        }
        if(joc.containsKey(DBURLKEY)){
            dbUrl = joc.get(DBURLKEY).toString();
        }
        if(dbName == null || dbName.isEmpty() ||
                dbUrl == null || dbUrl.isEmpty()){
            logger.error("Database not configured properly");
        } else {
            logger.info("mongourl:"+dbUrl+", database:"+dbName);
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
                Future<String> future = Future.future();
                future.fail("problem saving "+uloc.toString());
                resultHandler.handle(future);
            }
        });
    }

    //Note: one user id can only has one user local info. The new one will replace the old.
    public static void setUserLocal(MongoClient client, UserLocal uloc,
                                       Handler<AsyncResult<String>>resultHandler){
        JsonObject jo = uloc.toJsonObject();
        long uid = jo.getLong(UserLocal.UID);
        JsonObject query = new JsonObject().put(UserLocal.UID, uid);
        client.find(COLLECTION_USERLOCAL, query, res -> {
            if (res.succeeded()) {
                logger.info("removing old user locals");
                client.removeDocument(COLLECTION_USERLOCAL, query, removal->{
                    if(removal.succeeded()){
                        client.insert(COLLECTION_USERLOCAL, uloc.toJsonObject(), result->{
                            if(result.succeeded()){
                                resultHandler.handle(result);
                            }else{
                                logger.error("problem saving "+uloc.toString());
                                Future<String> future = Future.future();
                                future.fail("problem saving "+uloc.toString());
                                resultHandler.handle(future);
                            }
                        });
                    } else {
                        logger.error("Problem removing uid = "+uid);
                        //still trying to save
                        client.insert(COLLECTION_USERLOCAL, uloc.toJsonObject(), result->{
                            if(result.succeeded()){
                                resultHandler.handle(result);
                            }else{
                                logger.error("problem saving "+uloc.toString());
                                Future<String> future = Future.future();
                                future.fail("problem saving "+uloc.toString());
                                resultHandler.handle(future);
                            }
                        });
                    }
                });
            } else {
                logger.debug("save new user local:"+uloc.toString());
                client.insert(COLLECTION_USERLOCAL, uloc.toJsonObject(), result->{
                    if(result.succeeded()){
                        resultHandler.handle(result);
                    }else{
                        logger.error("problem saving "+uloc.toString());
                        Future<String> future = Future.future();
                        future.fail("problem saving "+uloc.toString());
                        resultHandler.handle(future);
                    }
                });
            }
        });
    }
}
