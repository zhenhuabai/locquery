package com.huleibo;

import common.Config;
import common.MongoDbHelper;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import locutil.UserLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import sun.misc.Signal;

import java.util.HashMap;
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
 * {cmd:getlocal, param:{uids:[1,2,...n], lang:string}}
 * {cmd:isroaming, param:{uids:long, lon:double, lat:double, level: n%}}
 */
public class LocationManager extends LocApp {
    private static final String[] cmds =
            {"upload","setlocal","getlocal","isroaming"};
    private HashMap<String, Function<JsonObject,Future<JsonObject>>> cmdDispatcher = new
            HashMap<>();
    private static final Logger logger = LogManager.getLogger(LocationManager.class);
    public void handle(Signal signalName) {
        logger.warn("Reveived signal:"+signalName.toString());
        if(signalName.getName().equalsIgnoreCase("term")){
            if(eb != null) {
                eb.close(handler -> {
                    logger.info("Application closed");
                });
            }
        }
    }
    private MongoClient mongoClient = null;

    public void init(){
        logger.debug("initializing LocationManager");
        mongoClient = MongoDbHelper.getInstance().requestClient(vertx);
        installCommandHandler();
    }
    @Override
    public void start() throws Exception {
        eb = vertx.eventBus();
        init();
        eb.consumer("Server:LocationManager", message -> {
            String uris = message.body().toString();
            try {
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
                                logger.debug("Failed saving user location:"+result.toString());
                                logger.debug("Failed saving user location msg:"+result.cause().getMessage());
                                message.reply("{\"error\":\"failed saving user location!\"}");
                            }
                        });
                    }else{
                        message.reply("{\"error\":\"command not supported!\"}");
                    }
                }else{
                    logger.error("Empty command!");
                    message.reply("{\"error\":\" no command!\"}");
                }
            }catch (Exception e){
                e.printStackTrace();
                message.reply("{\"error\":\""+e.getMessage()+"\"}");
            }
        });
    }
    @Override
    public void stop(){
        mongoClient.close();
        eb.close(handler->{
            logger.debug("stopped location manager");
        });
    }
    private void installCommandHandler(){
        //function handling upload command
        //we don't check parameters, assuming caller knows what is doing
        cmdDispatcher.put("upload",entries->{
            JsonObject ret = new JsonObject();
            Future<JsonObject> dbResult = Future.future();
            final JsonObject param = entries.getJsonObject("param");
            UserLocation ul = UserLocation.parseUserLocation(param);
            if(ul == null){
                logger.error("illegal parameters in user location");
                ret.put("error","illegal parameter");
                dbResult.fail(ret.toString());
            }else{
                //get city information from map server
                Future<Void> chain = Future.succeededFuture();
                chain.compose(v -> {
                    Future<String> cityFinding = Future.future();
                    StringBuffer sb = new StringBuffer();
                    sb.append(String.valueOf(ul.lat)).append(",")
                            .append(String.valueOf(ul.lon)).append(",lm");
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
                }).compose(city->{
                    //save user location info to db
                    JsonObject cityJo = new JsonObject(city);
                    ul.setCityInfo(cityJo);
                    MongoDbHelper.putUserLocation(mongoClient, ul, res->{
                        if(res.succeeded()){
                            ret.put("result",true);
                            dbResult.complete(ret);
                            logger.debug("upload:"+res.result());
                        }else{
                            logger.error("failed upload:"+res.result());
                            ret.put("result",false);
                            dbResult.fail(ret.toString());
                        }
                    });
                },dbResult);
            }
            return dbResult;
        });
    }
}
