package common;

import com.huleibo.LocationManager;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import locutil.UserLocal;
import locutil.UserLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.List;

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
            if (res.succeeded() && res.result().size()>0) {
                logger.info("removing old user locals");
                client.removeDocuments(COLLECTION_USERLOCAL, query, removal->{
                    if(removal.succeeded()){
                        logger.debug("insert new after removal");
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

    //Note: one user id can only has one user local info. The new one will replace the old.
    // {uid:xxx,locals:[{{cityinfo:{province:xxx,city:yyy},probability:n,lang:zh|cn,allowAnalyzer:true|false};
    public static void getUserLocal(MongoClient client, long uid, String lang,
                                    Handler<AsyncResult<JsonObject>>resultHandler){
        JsonObject query = new JsonObject().put(UserLocal.UID, uid);
        logger.debug("getUserLocal "+uid+","+lang);
        client.find(COLLECTION_USERLOCAL, query, res -> {
            if (res.succeeded() && res.result().size()>0) {
                List<JsonObject> ret = res.result();
                if(ret.size() > 1){
                    logger.warn("There are multiple occurrences of uid in local db!!"+uid);
                }
                //JsonObject reto = ret.get(0);
                JsonObject retJ = ret.get(0);
                JsonObject cityJ = null;
                JsonObject tmp = null;
                JsonArray locals = new JsonArray();
                Boolean analyzerEnabled = retJ.getBoolean(UserLocal.ANALYZERALLOWED);
                //if empty query set, or analyzer is not enabled
                tmp = retJ.getJsonObject(UserLocal.CITYINFO);
                String validLang = lang;
                logger.debug("tmp = "+tmp.toString()+","+lang);
                if (tmp != null) {
                    if (tmp.containsKey(lang)) {
                        cityJ = tmp.getJsonObject(lang);
                    } else {
                        validLang = "en";//default en
                        cityJ = tmp.getJsonObject(validLang);
                    }
                }
                //treat non-exist as empty not failure
                if (cityJ == null) {
                    cityJ = new JsonObject();
                }
                JsonObject rec = new JsonObject();
                JsonObject cityinfo = new JsonObject();
                cityinfo.put(UserLocal.LANG, validLang);
                cityinfo.put(UserLocal.CITYINFO, cityJ);
                cityinfo.put(UserLocal.PROBABILITY, 1.0);//alwasy 1.0
                locals.add(cityinfo);//just 1 here
                rec.put(UserLocal.LOCALS, locals);
                rec.put("uid", uid);
                rec.put(UserLocal.ANALYZERALLOWED,analyzerEnabled == null?true:analyzerEnabled);
                Future<JsonObject> future = Future.future();
                future.complete(rec);
                resultHandler.handle(future);
            }else{
                String message = res.cause() == null?"no user":res.cause().toString();
                logger.error("user not found or empty:"+uid+" :"+message);
                Future<JsonObject> future= Future.future();
                future.fail(message);
                resultHandler.handle(future);
            }
        });
    }
    //this is for analyzer to save the analyzed result
    public static void setAnalyzedLocal(MongoClient client, JsonObject jo,
                                    Handler<AsyncResult<String>>resultHandler){
        long uid = jo.getLong(UserLocal.UID);
        logger.info("save analyzed result for:"+uid);
        JsonObject query = new JsonObject().put(UserLocal.UID, uid);
        client.find(COLLECTION_USERLOCALANALYZED, query, res -> {
            if (res.succeeded() && res.result().size()>0) {
                logger.info("removing old analyzed result locals:"+res.result().toString());
                client.removeDocuments(COLLECTION_USERLOCALANALYZED, query, removal->{
                    if(removal.succeeded()){
                        client.insert(COLLECTION_USERLOCALANALYZED, jo, result->{
                            if(result.succeeded()){
                                resultHandler.handle(result);
                            }else{
                                logger.error("problem saving "+result.cause().toString());
                                Future<String> future = Future.future();
                                future.fail("problem saving "+result.cause().toString());
                                resultHandler.handle(future);
                            }
                        });
                    } else {
                        logger.error("Problem removing uid = "+uid);
                        //still trying to save
                        client.insert(COLLECTION_USERLOCALANALYZED, jo, result->{
                            if(result.succeeded()){
                                resultHandler.handle(result);
                            }else{
                                logger.error("problem saving "+uid+" "+result.cause().toString());
                                Future<String> future = Future.future();
                                future.fail("problem saving "+result.cause().toString());
                                resultHandler.handle(future);
                            }
                        });
                    }
                });
            } else {
                logger.debug("save analyzed user local:"+jo.toString());
                client.insert(COLLECTION_USERLOCALANALYZED, jo, result->{
                    if(result.succeeded()){
                        resultHandler.handle(result);
                    }else{
                        logger.error("problem saving "+jo.toString());
                        Future<String> future = Future.future();
                        future.fail("problem saving "+jo.toString());
                        resultHandler.handle(future);
                    }
                });
            }
        });
    }
    //retrieve a record of a uid
    // {uid:xxx,locals:[{{cityinfo:{province:xxx,city:yyy},probability:n,lang:zh};
    public static void getAnalyzedLocal(MongoClient client, long uid, String lang,
                                        Handler<AsyncResult<JsonObject>>resultHandler){
        logger.info("get analyzed result for:"+uid+","+lang);
        JsonObject query = new JsonObject().put(UserLocal.UID, uid);
        client.find(COLLECTION_USERLOCALANALYZED, query, res -> {
            if (res.succeeded() && res.result().size()>0) {
                List<JsonObject> ret = res.result();
                if(ret.size() > 1){
                    logger.warn("There are multiple occurrences of uid in local db!!"+uid);
                }
                JsonObject retJAnalyzed = ret.get(0);
                JsonArray anaResult = retJAnalyzed.getJsonArray("result");
                //if empty query set, or analyzer is not enabled
                JsonArray retResult = new JsonArray();
                JsonObject rec = new JsonObject();
                if(anaResult != null && anaResult.size() > 0){
                    //remove the unwanted database field
                    anaResult.forEach(o -> {
                        JsonObject itm = (JsonObject)o;
                        itm.remove("count"); //this count is for internal analyzing purpose
                        String cityinfoS = "";
                        String validLang = lang;
                        JsonObject tmp2 = itm.getJsonObject(UserLocal.CITYINFO);
                        logger.debug("analyzed "+validLang+" cityinfo:"+tmp2.toString());
                        //Cityinfo  saved in db as String
                        if(tmp2 != null){
                            if(tmp2.containsKey(validLang)) {
                                cityinfoS = tmp2.getString(lang);
                            }else{
                                validLang = "en";//default en
                                cityinfoS = tmp2.getString(validLang);
                            }
                        }
                        JsonObject cityinfoJ = new JsonObject(cityinfoS);
                        if(cityinfoJ == null){
                            cityinfoJ = new JsonObject();
                        }
                        JsonObject cityinfo = new JsonObject();
                        cityinfo.put(UserLocal.LANG,validLang);
                        cityinfo.put(UserLocal.CITYINFO,cityinfoJ);
                        cityinfo.put(UserLocal.PROBABILITY,itm.getDouble(UserLocal.PROBABILITY));
                        retResult.add(cityinfo);
                    });
                    rec.put(UserLocal.LOCALS,retResult);
                    rec.put("uid",uid);
                    Future<JsonObject> future = Future.future();
                    future.complete(rec);
                    resultHandler.handle(future);
                }else{
                    //query failed
                    logger.error("locals not created by analyzer yet.");
                    Future<JsonObject> future = Future.future();
                    future.fail("locals not analyzed");
                    resultHandler.handle(future);
                }
            }else{
                String message = res.cause() == null?"no user":res.cause().toString();
                logger.error("user not found or empty:"+uid+" :"+message);
                Future<JsonObject> future= Future.future();
                future.fail(message);
                resultHandler.handle(future);
            }
        });
    }
}
