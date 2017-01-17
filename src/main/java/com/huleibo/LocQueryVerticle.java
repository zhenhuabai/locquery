package com.huleibo;

import com.fasterxml.jackson.databind.ObjectMapper;
import common.Config;
import io.vertx.core.eventbus.EventBus;
import locutil.LocInfo;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import locsvc.CityQuery;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.StringWriter;

/**
 * Created by 白振华 on 2017/1/7.
 */
public class LocQueryVerticle extends AbstractVerticle{
    private static final Logger logger = LogManager.getLogger(LocQueryVerticle.class);
    //private final Logger Log = Logger.getLogger(this.getClass().getName());

    private EventBus eb;
    @Override
    public void start(Future<Void> fut) {
        eb = vertx.eventBus();
        int port = 8080;
        String ports = (String)Config.getInstance().getConfig().get("http.port").toString();
        if(ports != null && !ports.isEmpty()){
            try {
                port = Integer.valueOf(ports.toString());
            }catch (Exception e){
                port = 8080;
            }

        }
        logger.info("Service will work on :"+port);
        Router router = Router.router(vertx);
        router.route("/").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response
                    .putHeader("content-type", "text/html")
                    .end("<h1>Hello, thanks for visiting, still under construction</h1>");
        });
        router.route("/api/city*").handler(BodyHandler.create());
        router.get("/api/city").handler(this::remoteQueryCity);

        vertx
                .createHttpServer()
                .requestHandler(router::accept)
                .listen(
                        // Retrieve the port from the configuration,
                        // default to 8080.
                        port,
                        result -> {
                            if (result.succeeded()) {
                                fut.complete();
                                logger.info("Serivce Started");
                            } else {
                                fut.fail(result.cause());
                            }
                        }
                );
    }
    private void remoteQueryCity(RoutingContext routingContext) {
         LocInfo loc = null;
        StringWriter out = new StringWriter();
        try {
            logger.info("Handling request:" + routingContext.request().toString());
            String lon = routingContext.request().getParam("lat").toString();
            String lat = routingContext.request().getParam("lon").toString();
            String lang = routingContext.request().getParam("lang");

            if(lat != null && lon != null && !lat.isEmpty() && !lon.isEmpty()) {
                //TODO: hardcoded map server so far
                StringBuffer sb = new StringBuffer();
                sb.append(lat.trim()).append(",").append(lon.trim());
                if (lang != null && !lang.isEmpty()) {
                    sb.append(",").append(lang.trim());
                }
                eb.send("Server:China",sb.toString(), reply -> {
                    if (reply.succeeded()) {
                        logger.info(String.format("[%s, %s]->%s", lat, lon, reply.result().body().toString()));
                        routingContext.response()
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end(reply.result().body().toString());
                    } else {
                        logger.warn("Server no reply for:"+sb);
                        routingContext.response()
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end("Error: Map server not ready!");
                    }
                });
            }else{
                logger.warn("Illegal parameters");
                routingContext.response().setStatusCode(400).end();
            }
        } catch (Exception e){
            e.printStackTrace();
            logger.warn("Problem handling request:"+routingContext.request().toString());
            routingContext.response().setStatusCode(400).end();
        }
    }
    private void queryCity(RoutingContext routingContext) {
        LocInfo loc = null;
        StringWriter out = new StringWriter();
        try {
            logger.info("Handling request:" + routingContext.request().toString());
            String lat = routingContext.request().getParam("lat").toString();
            String lon = routingContext.request().getParam("lon").toString();
            double latd = new Double(lat);
            double lond = new Double(lon);
            String[] res = CityQuery.find(latd, lond);
            loc = new LocInfo(res);
            ObjectMapper om = new ObjectMapper();
            om.writeValue(out, loc.data);
            logger.info(String.format("[%f, %f]->%s",latd, lond, out.toString()));
            routingContext.response()
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(out.toString());
        }catch (Exception e){
            e.printStackTrace();
            logger.warn("Problem handling request:"+routingContext.request().toString());
            routingContext.response()
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end("Ooops, Error:( No info found for your input!");
        }
    }
    private void pingService(){
        int port = config().getInteger("http.port", 8080);
        vertx.createHttpClient().getNow(port, "localhost", "/api/city?lat=109.594513&lon=34.644989",
                response -> logger.info("Service Ready")
                );
    }
}
