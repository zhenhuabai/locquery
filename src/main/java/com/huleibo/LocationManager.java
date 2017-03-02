package com.huleibo;

import common.Config;
import common.MongoDbHelper;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import locutil.LocationAnalyzer;
import locutil.UserLocal;
import locutil.UserLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import sun.misc.Signal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Created by 白振华 on 2017/2/8.
 * This class handles location related operations, like uploading location,
 * setting user local, retrieving user local, etc.
 * This instance will run as a single process, facilitating multiple instances
 * in the future.
 * This server receives & processes command string in format:
 * {cmd:upload, param:{uid:long,lon:double,lat:double,timestamp:long}}
 * {cmd:setlocal, param:{uid:long,country:string, province:string,city:string,county:string}}
 * {cmd:getlocals, param:{uids:[1,2,...n], lang:string}}
 * {cmd:isroaming, param:{uid:long, lon:double, lat:double, probability: 0.x}}
 *
 * return {result:"OK"} if command executed successfully
 * return {result:"error:[reason]"} if command failed
 */
public class LocationManager extends LocApp {
    private boolean initialized = false;
    private MongoClient mongoClient = null;
    private HashMap<String, Function<JsonObject,Future<JsonObject>>> cmdDispatcher = new
            HashMap<>();
    private static final Logger logger = LogManager.getLogger(LocationManager.class);
    public void handle(Signal signalName) {
        logger.warn("Reveived signal:"+signalName.toString());
        if(signalName.getName().equalsIgnoreCase("term")){
            vertx.close(res->{
                logger.warn("Closed Location manager server!");
            });
        }
    }

    public void init(Future<Void> fut){
        logger.debug("initializing LocationManager");
        vertx.executeBlocking(future -> {
            logger.debug("initializing mongoclient and commands");
            mongoClient = MongoDbHelper.getInstance().requestClient(vertx);
            LocationAnalyzer.startLocalLocation(mongoClient);
            installCommandHandler();
            Set<String> keys = cmdDispatcher.keySet();
            keys.forEach(s ->{ logger.debug("installed cmd-->"+s);});
            future.complete();
        }, res -> {
            fut.complete();
            initialized = true;
            Set<String> keys = cmdDispatcher.keySet();
            keys.forEach(s ->{ logger.debug("installed cmd-->"+s);});
            logger.info("Location Manager initialized."+mongoClient);
            logger.info("Location Manager initialized.");
        });
    }
    public void init(){
        logger.debug("initializing LocationManager");
        if(!initialized) {
            vertx.executeBlocking(future -> {
                mongoClient = MongoDbHelper.getInstance().requestClient(vertx);
                LocationAnalyzer.startLocalLocation(mongoClient);
                installCommandHandler();
                Set<String> keys = cmdDispatcher.keySet();
                keys.forEach(s ->{ logger.debug("installed cmd-->"+s);});
                future.complete();
            }, res -> {
                initialized = true;
            });
        }else{
            logger.info("Location Manager was initialized before.");
        }
    }
    @Override
    //public void start(Future<Void>fut) throws Exception {
    public void start() throws Exception {
        eb = vertx.eventBus();
        init();
        eb.consumer("Server:LocationManager", message -> {
            String uris = message.body().toString();
            try {
                if(!initialized){
                    throw new Exception("Location Manager still in initialization");
                }
                JsonObject cmd = new JsonObject(uris);
                logger.debug("received:"+cmd.toString());
                if(!cmd.isEmpty()){
                    String cmdS = cmd.getString("cmd");
                    if(cmdDispatcher.containsKey(cmdS)){
                        Future<JsonObject> res = cmdDispatcher.get(cmdS).apply(cmd);
                        res.setHandler(result->{
                            if(result.succeeded()){
                                logger.debug(cmd.toString()+"->"+result.result().toString());
                                message.reply(result.result().toString());
                            }else{
                                logger.debug("Failed operation:"+cmdS+" msg:"+result.cause().getMessage());
                                JsonObject errorResult = new JsonObject();
                                String errmsg = "error:"+result.cause().getMessage();
                                errorResult.put("result",errmsg);
                                message.reply(errorResult.toString());
                            }
                        });
                    }else{
                        JsonObject errorResult = new JsonObject();
                        String errmsg = "error: command not supported";
                        errorResult.put("result",errmsg);
                        message.reply(errorResult.toString());
                        logger.error(errmsg);
                    }
                }else{
                    JsonObject errorResult = new JsonObject();
                    String errmsg = "error: Empty Command";
                    errorResult.put("result",errmsg);
                    message.reply(errorResult.toString());
                    logger.error(errmsg);
                }
            }catch (Exception e){
                e.printStackTrace();
                JsonObject errorResult = new JsonObject();
                String errmsg = "error:"+e.getMessage();
                errorResult.put("result",errmsg);
                message.reply(errorResult.toString());
                logger.error(errmsg);
            }
        });
    }
    @Override
    public void stop(){
        if(mongoClient != null) {
            mongoClient.close();
        }
    }
    private void installCommandHandler() {
        //function handling upload command
        //we don't check parameters, assuming caller knows what is doing
        logger.debug("installing commands");
        cmdDispatcher.put("upload", entries -> {
            JsonObject ret = new JsonObject();
            Future<JsonObject> dbResult = Future.future();
            final JsonObject param = entries.getJsonObject("param");
            UserLocation ul = UserLocation.parseUserLocation(param);
            if (ul == null) {
                logger.error("illegal parameters in user location");
                ret.put("error", "illegal parameter");
                dbResult.fail(ret.toString());
            } else {
                //get city information from map server
                Future<Void> chain = Future.succeededFuture();
                chain.compose(v -> {
                    Future<String> cityFinding = Future.future();
                    StringBuffer sb = new StringBuffer();
                    sb.append(String.valueOf(ul.lon)).append(",")
                            .append(String.valueOf(ul.lat)).append(",lm");
                    eb.send("Server:China", sb.toString(), reply -> {
                        if (reply.succeeded()) {
                            logger.info(String.format("[%s]->%s", param.toString(), reply.result().body().toString()));
                            cityFinding.complete(reply.result().body().toString());
                        } else {
                            cityFinding.fail("No reply from MapServer");
                            logger.warn("Server no reply for:" + sb);
                        }
                    });
                    return cityFinding;
                }).compose(city -> {
                    //save user location info to db
                    JsonObject cityJo = new JsonObject(city);
                    if(cityJo != null && !cityJo.isEmpty()) {
                        ul.setCityInfo(cityJo);
                        MongoDbHelper.putUserLocation(mongoClient, ul, res -> {
                            if (res.succeeded()) {
                                ret.put("result", "OK");
                                dbResult.complete(ret);
                                logger.debug("upload:" + res.result());
                            } else {
                                logger.error("failed upload:" + res.result());
                                ret.put("result", "error:upload");
                                dbResult.fail(ret.toString());
                            }
                        });
                    }else{
                        String err = String.format("No city by[%f,%f]",ul.lon,ul.lat);
                        logger.warn(err);
                        dbResult.fail(err);
                    }
                }, dbResult);
            }
            return dbResult;
        });
        //processing setlocal command
        cmdDispatcher.put("setlocal", entries -> {
            JsonObject ret = new JsonObject();
            Future<JsonObject> dbResult = Future.future();
            final JsonObject param = entries.getJsonObject("param");
            UserLocal ul = UserLocal.parseUserLocal(param);
            if (ul == null) {
                logger.error("illegal parameters in user local");
                ret.put("error", "illegal parameter");
                dbResult.fail(ret.toString());
            } else {
                //get city information from map server
                Future<Void> chain = Future.succeededFuture();
                chain.compose(v -> {
                    //save user location info to db
                    MongoDbHelper.setUserLocal(mongoClient, ul, res -> {
                        if (res.succeeded()) {
                            ret.put("result", "OK");
                            dbResult.complete(ret);
                            logger.debug("upload:" + res.result());
                        } else {
                            logger.error("failed setlocal:" + res.result());
                            ret.put("result", "error:setlocal");
                            dbResult.fail(ret.toString());
                        }
                    });
                }, dbResult);
            }
            return dbResult;
        });
        //process get user local cities.
        /*
         *the returned result on success:
         * {result:[{uid:xxx,locals:[{{cityinfo:{province:xxx,city:yyy},probability:n,lang:zh|cn}],{....}}];
         */
        cmdDispatcher.put("getlocals", entries -> {
            Future<JsonObject> dbResult = Future.future();
            JsonObject okResult = new JsonObject();
            JsonArray resultSet = new JsonArray();
            final JsonObject param = entries.getJsonObject("param");
            final String lang = entries.getString("lang");//.trim().toLowerCase();
            logger.debug("cmd = " + entries.toString() + ", lang=" + lang);
            JsonArray uids = param.getJsonArray("uids");
            List<Future> ftList = new ArrayList<>();
            HashMap<Integer, Future> uidFutures = new HashMap<>();
            for (int i = 0; i < uids.size(); i++) {
                int lid = uids.getInteger(i);
                Future<Void> uidf = Future.future();
                uidFutures.put(lid, uidf);
                ftList.add(i, uidf);
            }
            uids.forEach(id -> {
                Integer lid = (Integer) id;
                //must returned the pre-set local?
                MongoDbHelper.getUserLocal(mongoClient, lid, lang, result -> {
                    if (result.succeeded()) {
                        JsonObject locals = result.result();
                        logger.debug("preset local:" + locals.toString());
                        Boolean ae = locals.getBoolean(UserLocal.ANALYZERALLOWED);
                        locals.remove(UserLocal.ANALYZERALLOWED);//not user visible
                        if (ae == null || ae) {
                            MongoDbHelper.getAnalyzedLocal(mongoClient, lid, lang, anaresult -> {
                                if (anaresult.succeeded()) {
                                    JsonObject anaLocals = anaresult.result();
                                    logger.debug("added analyzed local:" + anaresult.toString());
                                    resultSet.add(anaLocals);
                                    uidFutures.get(lid).complete();
                                } else {
                                    //error! but treated as successfull, using the assigned one
                                    resultSet.add(locals);
                                    uidFutures.get(lid).complete();
                                    logger.error("error from getAnalyzedLocal:" + anaresult.cause().toString());
                                    logger.error("use the assigned one instead.");
                                }
                            });
                        } else {
                            logger.debug("added preset local:" + locals.toString());
                            resultSet.add(locals);
                            uidFutures.get(lid).complete();
                        }
                    } else {
                        logger.error("error from getUserLocal:" + result.cause().toString());
                        //try analyzed result as well
                        MongoDbHelper.getAnalyzedLocal(mongoClient, lid, lang, anaresult -> {
                            if (anaresult.succeeded()) {
                                JsonObject anaLocals = anaresult.result();
                                logger.debug("added analyzed local:" + anaresult.result().toString());
                                resultSet.add(anaLocals);
                                uidFutures.get(lid).complete();
                            } else {
                                //error! but treated as successfull, using empty result
                                JsonObject emptyRec = new JsonObject();
                                JsonArray emptyResult = new JsonArray();
                                emptyRec.put(UserLocal.LOCALS, emptyResult);
                                emptyRec.put("uid", lid);
                                resultSet.add(emptyRec);
                                uidFutures.get(lid).complete();
                                logger.error("error from getAnalyzedLocal:" + anaresult.cause().toString());
                            }
                        });
                    }
                });
            });
            CompositeFuture.all(ftList).setHandler(handle -> {
                okResult.put("result", "OK");
                okResult.put("data", resultSet);
                logger.debug("getlocals:" + okResult.toString());
                dbResult.complete(okResult);
            });
            return dbResult;
        });
        //check if a user is outside his/her "local cities"
        /*
         * {cmd:isnonlocal, param:{uid:long, lon:double, lat:double, probability: 0.x}}
         * the returned result on success:
         * {result:{uid:xxx,nonlocals:{true|false}}
         */
        cmdDispatcher.put("isnonlocal", entries -> {
            JsonObject ret = new JsonObject();
            Future<JsonObject> dbResult = Future.future();
            final JsonObject param = entries.getJsonObject("param");
            String uid = param.getString("uid");
            Double lon = param.getDouble("lon");
            Double lat = param.getDouble("lat");
            Double probability = param.getDouble("probability");
            if (uid == null || lon == null || lat == null || probability == null) {
                logger.error("illegal parameters in user location");
                ret.put("error", "illegal parameter");
                dbResult.fail(ret.toString());
            } else {
                //get city information from map server
                Future<Void> chain = Future.succeededFuture();
                chain.compose(v -> {
                    Future<String> cityFinding = Future.future();
                    StringBuffer sb = new StringBuffer();
                    sb.append(String.valueOf(lat)).append(",")
                            .append(String.valueOf(lon)).append(",lm");
                    eb.send("Server:China", sb.toString(), reply -> {
                        if (reply.succeeded()) {
                            logger.info(String.format("[%s]->%s", param.toString(), reply.result().body().toString()));
                            cityFinding.complete(reply.result().body().toString());
                        } else {
                            cityFinding.fail("No reply from MapServer");
                            logger.warn("Server no reply for:" + sb);
                        }
                    });
                    return cityFinding;
                }).compose(city -> {
                    //We've known the city names, check them in history
                    JsonObject foundcity =  new JsonObject(city);
                    logger.debug("City from MapServer :"+foundcity.toString());
                    if (foundcity == null || foundcity.isEmpty()) {
                        logger.error("no city info found:"+foundcity.toString());
                        dbResult.fail("no city found from MapServer");
                    } else {
                        //fetch the history
                        LocationAnalyzer.getInstance().parseUserLocal(Long.parseLong(uid), probability, mongoClient, historyCities -> {
                            if (historyCities.succeeded()) {
                                JsonArray citylist = historyCities.result().getJsonArray("result");
                                logger.debug("history city info:"+citylist.toString());
                                boolean islocal = false;
                                search_qualified_history:
                                for (Object c : citylist) {
                                    JsonObject acity = (JsonObject) c;
                                    JsonObject cityinfo = acity.getJsonObject("cityinfo");
                                    logger.debug("Comparing with:"+cityinfo);
                                    String[] supportedLang = {"zh", "en"};
                                    for (int i = 0; i < supportedLang.length; i++) {
                                        String langcityS = cityinfo.getString(supportedLang[i]);
                                        String langcityFoundS = foundcity.getString(supportedLang[i]);
                                        if(langcityFoundS.equals(langcityS)){
                                            islocal = true;
                                            break search_qualified_history;
                                        }
                                        /*
                                        JsonObject langcity = new JsonObject(cityinfo.getString(supportedLang[i]));
                                        JsonObject langcityFound = new JsonObject(foundcity.getString(supportedLang[i]));
                                        if (langcity != null) {
                                            String province = langcity.getString(UserLocal.PROVINCE);
                                            String cit = langcity.getString(UserLocal.CITY);
                                            if (province.equalsIgnoreCase(langcityFound.getString(UserLocal.PROVINCE)) &&
                                                    cit.equalsIgnoreCase(langcityFound.getString(UserLocal.CITY))
                                                    ) {
                                                //found match
                                                isnonlocal = true;
                                                break search_qualified_history;
                                            }
                                        }
                                        */
                                    }
                                }
                                //we are checking non-local, reverse the result
                                JsonObject rslt = new JsonObject().put("result", "OK").put("data",!islocal);
                                dbResult.complete(rslt);
                            } else {
                                logger.error("Problem searching in location history");
                                dbResult.fail("problem searching history");
                            }
                        });
                    }
                }, dbResult);
            }
            return dbResult;
        });
    }

}
