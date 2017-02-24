package com.huleibo;

import com.fasterxml.jackson.databind.ObjectMapper;
import common.Config;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
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
        router.get("/api/userlocal").handler(this::getUserLocal);
        router.route("/api/isnonlocal*").handler(BodyHandler.create());
        router.get("/api/isnonlocal").handler(this::isUserRoaming);

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
                routingContext.response().setStatusMessage("Illegal parameters").setStatusCode(400).end();
            }
        } catch (Exception e){
            e.printStackTrace();
            logger.warn("Problem handling request:"+routingContext.request().toString());
            routingContext.response().setStatusCode(400).end();
        }
    }
    public void isUserRoaming(RoutingContext routingContext) {
        try {
            String request = routingContext.request().absoluteURI();
            logger.debug("handling isnonlocal:"+request);
            if(request.contains("uid")) {
                String uid = routingContext.request().getParam("uid").toString();
                String prob = routingContext.request().getParam("probability").toString();
                String location = routingContext.request().getParam("location").toString();
                Double probability = Double.parseDouble(prob);
                String[] locs = location.split(",");
                Double lon = Double.parseDouble(locs[1]);
                Double lat = Double.parseDouble(locs[0]);
                JsonObject param = new JsonObject();
                param.put("uid",uid);
                param.put("lon",lon);
                param.put("lat",lat);
                param.put("probability",probability);
                JsonObject isroaming = new JsonObject().put("cmd","isnonlocal")
                        .put("param",param);
                if(uid == null || location == null || locs == null ||
                        lon == null || lat == null || probability<0||probability>1){
                    logger.error("parameters not properly set");
                    throw new Exception("Illegal parameters");
                }
                eb.send("Server:LocationManager", isroaming.toString(), reply -> {
                    if (reply.succeeded()) {
                        logger.info(String.format("[%s]->%s", "isnonlocal", reply.result().body().toString()));
                        routingContext.response()
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end(reply.result().body().toString());
                    } else {
                        logger.warn("Server no reply for:isroaming");
                        routingContext.response()
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end("Error: LocationManager server not ready!");
                    }
                });
            }else{
                logger.warn("Illegal parameters");
                routingContext.response().setStatusMessage("Illegal parameters").setStatusCode(400).end();
            }
        } catch (Exception e){
            e.printStackTrace();
            logger.warn("Problem handling request:"+routingContext.request().toString());
            routingContext.response().setStatusMessage(e.toString()).setStatusCode(400).end();
        }
    }
    public void getUserLocal(RoutingContext routingContext) {
        try {
            String request = routingContext.request().absoluteURI();
            logger.debug("handling getUserLocal:"+request);
            if(request.contains("uid")) {
                String uid = routingContext.request().getParam("uid").toString();
                String lang = routingContext.request().getParam("lang").toString();
                String[]uids = uid.split(",");
                JsonArray uidA = new JsonArray();
                for (int i = 0; i < uids.length; i++) {
                    Integer v = Integer.parseInt(uids[i]);
                    uidA.add(v);
                }
                JsonObject param = new JsonObject();
                param.put("uids",uidA);
                JsonObject getlocal = new JsonObject().put("cmd","getlocals")
                        .put("param",param)
                        .put("lang",lang);
                logger.debug("process uid:"+uid+" lang:"+lang);
                if(uid == null || lang == null || lang.toLowerCase().matches("zh\\|en")){
                    logger.error("parameters not properly set");
                    throw new Exception("Illegal parameters");
                }
                eb.send("Server:LocationManager", getlocal.toString(), reply -> {
                    if (reply.succeeded()) {
                        logger.info(String.format("[%s]->%s", "getlocal", reply.result().body().toString()));
                        routingContext.response()
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end(reply.result().body().toString());
                    } else {
                        logger.warn("Server no reply for:getlocal");
                        routingContext.response()
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end("Error: LocationManager server not ready!");
                    }
                });
            }else{
                logger.warn("Illegal parameters");
                routingContext.response().setStatusMessage("Illegal parameters").setStatusCode(400).end();
            }
        } catch (Exception e){
            e.printStackTrace();
            logger.warn("Problem handling request:"+routingContext.request().toString());
            routingContext.response().setStatusMessage(e.toString()).setStatusCode(400).end();
        }
    }
    public void setUserLocal(RoutingContext routingContext) {
        try {
            String request = routingContext.request().absoluteURI();
            logger.debug("handling setUserLocal:"+request);
            String body = routingContext.getBodyAsString();
            JsonObject jo = routingContext.getBodyAsJson();
            JsonObject setlocal = new JsonObject().put("cmd","setlocal").put("param",jo);
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
        } catch (Exception e){
            e.printStackTrace();
            logger.warn("Problem handling request:"+routingContext.request().toString());
            routingContext.response().setStatusMessage("Illegal parameters").setStatusCode(400).end();
        }
    }
    public void uploadUserLocation(RoutingContext routingContext) {
        try {
            String request = routingContext.request().absoluteURI();
            logger.debug("handling uploadUserLocation");
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
        } catch (Exception e){
            e.printStackTrace();
            logger.warn("Problem handling request:"+routingContext.request().toString());
            routingContext.response().setStatusCode(400).setStatusMessage("Illegal parameters").end();
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
