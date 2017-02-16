package com.huleibo;

import common.Config;
import common.MongoDbHelper;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

/**
 * Created by 白振华 on 2017/2/8.
 * This class handles location related operations, like uploading location,
 * setting user local, retrieving user local, etc.
 * This instance will run as a single process, so facilitating multiple instance
 * in the future.
 */
public class LocationManager extends AbstractVerticle {
    private static final Logger logger = LogManager.getLogger(LocationManager.class);
    private EventBus eb;
    private MongoClient mongoClient = null;

    public void init(){
        logger.debug("initializing database");
        mongoClient = MongoDbHelper.getInstance().requestClient(vertx);
    }
    @Override
    public void start() throws Exception {
        eb = vertx.eventBus();
        init();
        eb.consumer("Server:LocationManager", message -> {
            String uris = message.body().toString();
            try {

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
}
