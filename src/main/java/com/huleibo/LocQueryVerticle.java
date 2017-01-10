package com.huleibo;

import com.fasterxml.jackson.databind.ObjectMapper;
import common.Config;
import common.LocInfo;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import jdk.nashorn.internal.parser.JSONParser;
import locsvc.CityQuery;
import locutil.GlobeDataStore;
import org.apache.commons.io.output.StringBuilderWriter;
import org.json.simple.JSONObject;

import java.io.StringWriter;
import java.util.logging.Logger;

/**
 * Created by 白振华 on 2017/1/7.
 */
public class LocQueryVerticle extends AbstractVerticle{
    private final Logger Log = Logger.getLogger(this.getClass().getName());

    private boolean mapservermode = true;
    @Override
    public void start(Future<Void> fut) {
        if(mapservermode){
            NetServer server = vertx.createNetServer();
            server.connectHandler(socket -> {
                socket.handler(buffer -> {
                    StringWriter out = new StringWriter();
                    String params = buffer.toString();
                    System.out.println("Recieved:"+params);
                    /*
                    JSONObject jo = new JSONObject(params);
                    String lat = jo.get("lat").toString();
                    String lon = jo.get("lon").toString();
                    double la = Double.valueOf(lat);
                    double lo = Double.valueOf(lon);
                    */
                    double la=0, lo=0;
                    try {
                        LocInfo li = GlobeDataStore.getInstance().findCityDirect(la, lo);
                        ObjectMapper om = new ObjectMapper();
                        om.writeValue(out, li.data);
                        socket.write("this is ");
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                });
            });
            server.listen(4321, "localhost", res -> {
                if (res.succeeded()) {
                    System.out.println("Map Server is now listening!");
                } else {
                    System.out.println("Failed to bind!");
                }
            });
            return;
        }
        int port = config().getInteger("http.port", 8080);
        Log.info("read port:"+port);
        // Create a router object.
        Router router = Router.router(vertx);

        // Bind "/" to our hello message - so we are still compatible.
        router.route("/").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response
                    .putHeader("content-type", "text/html")
                    .end("<h1>Hello, thanks for visiting, still under construction</h1>");
        });
        router.route("/api/city*").handler(BodyHandler.create());
        router.get("/api/city").handler(this::queryCity);

        vertx
                .createHttpServer()
                .requestHandler(router::accept)
                .listen(
                        // Retrieve the port from the configuration,
                        // default to 8080.
                        config().getInteger("http.port", 8080),
                        result -> {
                            if (result.succeeded()) {
                                fut.complete();
                                Log.info("Serivce Started");
                                pingService();
                            } else {
                                fut.fail(result.cause());
                            }
                        }
                );
    }
    private void queryCity(RoutingContext routingContext) {
        LocInfo loc = null;
        StringWriter out = new StringWriter();
        try {
            Log.info("Handling request:" + routingContext.request().toString());
            String lat = routingContext.request().getParam("lat").toString();
            String lon = routingContext.request().getParam("lon").toString();
            double latd = new Double(lat);
            double lond = new Double(lon);
            String[] res = CityQuery.find(latd, lond);
            loc = new LocInfo(res);
            ObjectMapper om = new ObjectMapper();
            om.writeValue(out, loc.data);
            Log.info(String.format("[%f, %f]->%s",latd, lond, out.toString()));
            routingContext.response()
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(out.toString());
        }catch (Exception e){
            e.printStackTrace();
            Log.warning("Problem handling request:"+routingContext.request().toString());
            routingContext.response()
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end("Ooops, Error:( No info found for your input!");
        }
    }
    private void pingService(){
        int port = config().getInteger("http.port", 8080);
        vertx.createHttpClient().getNow(port, "localhost", "/api/city?lat=109.594513&lon=34.644989",
                response -> Log.info("Service Ready")
                );
    }
}
