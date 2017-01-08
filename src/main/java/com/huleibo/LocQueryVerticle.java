package com.huleibo;

import com.fasterxml.jackson.databind.ObjectMapper;
import common.LocInfo;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import locsvc.CityQuery;
import locutil.GlobeDataStore;
import org.apache.commons.io.output.StringBuilderWriter;

import java.io.StringWriter;
import java.util.logging.Logger;

/**
 * Created by 白振华 on 2017/1/7.
 */
public class LocQueryVerticle extends AbstractVerticle{
    public LocQueryVerticle (){
        super();
        GlobeDataStore.getInstance();
    }
    private final Logger Log = Logger.getLogger(this.getClass().getName());
    @Override
    public void start(Future<Void> fut) {
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

        // Create the HTTP server and pass the "accept" method to the request handler.
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
                                //pingService();
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
            //e.printStackTrace();
            Log.warning("Problem handling request."+e.getMessage());
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
