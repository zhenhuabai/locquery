package com.huleibo;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by 白振华 on 2017/2/8.
 */
public class LocationManager extends AbstractVerticle {
    private static final Logger logger = LogManager.getLogger(LocationManager.class);
    private EventBus eb;

    public void init(){
        logger.debug("initializing database");
        JsonObject config = new JsonObject();
        config.put("db_name","test");
        config.put("connection_string","mongodb://localhost:27017");
        MongoClient mongoClient = MongoClient.createShared(vertx, config);
        JsonObject document = new JsonObject().put("title", "The Hobbit2");
        mongoClient.insert("books", document, res -> {
            if (res.succeeded()) {
                String id = res.result();
                logger.debug("Saved book with id " + id);
            } else {
                logger.debug("insert not ok");
                res.cause().printStackTrace();
            }
        });
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
        eb.close(handler->{
            logger.debug("stopped location manager");
        });
    }
}
