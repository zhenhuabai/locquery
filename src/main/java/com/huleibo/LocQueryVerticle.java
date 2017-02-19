package com.huleibo;

import com.fasterxml.jackson.databind.ObjectMapper;
import common.Config;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import locutil.LocInfo;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import locsvc.CityQuery;
import locutil.UserLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.io.File;
import java.io.StringWriter;

/**
 * Created by 白振华 on 2017/1/7.
 */
public class LocQueryVerticle extends LocApp {
    private HttpServer theServer;
    public void handle(Signal signalName) {
        logger.warn("Reveived signal:"+signalName.toString());
        if(signalName.getName().equalsIgnoreCase("term")){
            logger.warn("TERM Reveived! Exiting app");
            if(theServer != null) {
                theServer.close();
            }
        }
    }
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
        router.route("/api/weather*").handler(BodyHandler.create());
        router.get("/api/weather").handler(this::remoteQueryWeather);
        router.route("/api/userlocation*").handler(BodyHandler.create());
        router.post("/api/userlocation").handler(this::uploadUserLocation);
        router.route("/api/userlocal*").handler(BodyHandler.create());
        router.put("/api/userlocal").handler(this::setUserLocal);
        /*
        router.get("/api/userlocal").handler(this::isUserLocal);
        router.route("/api/userroaming*").handler(BodyHandler.create());
        router.get("/api/userroaming").handler(this::isUserRoaming);
        */

        theServer = vertx.createHttpServer();
                theServer
                .requestHandler(router::accept)
                .listen(
                        // Retrieve the port from the configuration,
                        // default to 8080.
                        port,
                        result -> {
                            if (result.succeeded()) {
                                installSignal();
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

    private void remoteQueryWeather(RoutingContext routingContext) {
        try {
            String request = routingContext.request().absoluteURI();
            if(request.contains("location")) {
                eb.send("Server:Weather", request, reply -> {
                    if (reply.succeeded()) {
                        logger.info(String.format("[%s]->%s", request, reply.result().body().toString()));
                        routingContext.response()
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end(reply.result().body().toString());
                    } else {
                        logger.warn("Server no reply for:"+request);
                        routingContext.response()
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end("Error: Weather server not ready!");
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
    public void setUserLocal(RoutingContext routingContext) {
        try {
            String request = routingContext.request().absoluteURI();
            logger.debug("handling setUserLocal");
            if(!request.contains("uid")) {
                String body = routingContext.getBodyAsString();
                logger.debug("body string="+body);
                JsonObject jo = routingContext.getBodyAsJson();
                JsonObject setlocal = new JsonObject().put("cmd","setlocal").put("param",jo);
                logger.debug("local = "+jo.toString());
                eb.send("Server:LocationManager", setlocal.toString(), reply -> {
                    if (reply.succeeded()) {
                        logger.info(String.format("[%s]->%s", "setlocal", reply.result().body().toString()));
                        routingContext.response()
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end(reply.result().body().toString());
                    } else {
                        logger.warn("Server no reply for:setlocal");
                        routingContext.response()
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end("Error: LocationManager server not ready!");
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
    public void uploadUserLocation(RoutingContext routingContext) {
        try {
            String request = routingContext.request().absoluteURI();
            logger.debug("handling uploadUserLocation");
            if(!request.contains("uid")) {
                String body = routingContext.getBodyAsString();
                logger.debug("body string="+body);
                JsonObject jo = routingContext.getBodyAsJson();
                JsonObject upload = new JsonObject().put("cmd","upload").put("param",jo);
                logger.debug("upload = "+jo.toString());
                eb.send("Server:LocationManager", upload.toString(), reply -> {
                    if (reply.succeeded()) {
                        logger.info(String.format("[%s]->%s", "upload", reply.result().body().toString()));
                        routingContext.response()
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end(reply.result().body().toString());
                    } else {
                        logger.warn("Server no reply for:upload");
                        routingContext.response()
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end("Error: LocationManager server not ready!");
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
    private void pingService(){
        int port = config().getInteger("http.port", 8080);
        vertx.createHttpClient().getNow(port, "localhost", "/api/city?lat=109.594513&lon=34.644989",
                response -> logger.info("Service Ready")
                );
    }

    public void stop(){
        if(eb != null) {
            eb.close(handler -> {
                logger.debug("stopped Http Server");
            });
        }
        if(theServer != null) {
            theServer.close();
        }
    }
}
