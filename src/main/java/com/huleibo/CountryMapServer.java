package com.huleibo;

import common.Config;
import io.vertx.core.Vertx;
import locutil.LocInfo;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.net.NetServer;
import locutil.GlobeDataStore;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by 白振华 on 2017/1/11.
 */
public class CountryMapServer extends AbstractVerticle{
    private int port;
    private String mappath;
    public static boolean inDebug = false;

    static public void main(String[] args){
        Vertx vertxx = Vertx.vertx();
        vertxx.deployVerticle(CountryMapServer.class.getName());
    }
    Logger Log = Logger.getLogger(this.getClass().getName());
    private void setupEnv(){
        int dbg = config().getInteger("debug",0);
        if(dbg == 1) {
            JSONArray maps = Config.getInstance().getMapConfig();
            for (int i = 0; i < maps.size(); i++) {
                JSONObject map = (JSONObject) maps.get(i);
                String name = map.get("name").toString();
                String path1 = map.get("outline").toString();
                port = 18080;
                mappath = "China";
            }
        }else{
            Map<String, String> env = System.getenv();
            try {
                String ports = env.get("port").toString();
                if (ports == null || ports.isEmpty()) {
                    Log.severe("No port defined, exit");
                    System.exit(1000);
                }
                port = Integer.valueOf(ports);
                mappath = env.get("map");
                if (mappath == null || mappath.isEmpty()) {
                    Log.severe("No map, exit");
                    System.exit(1001);
                }
            }catch (Exception e){
                Log.severe("Error getting params");
                System.exit(3);
            }
        }
        Log.info("Map["+mappath+"] will service in "+port);
    }
    @Override
    public void start() throws Exception {
        setupEnv();
        NetServer server = vertx.createNetServer();
        server.connectHandler(socket -> {
            socket.handler(buffer -> {
                //strict check should be performed outside!!!
                String s = buffer.toString();
                Log.info("received: " + s);
                String ds[] = s.split(",");
                double lon, lat;
                try {
                    lat = new Double(ds[0]);
                    lon = new Double(ds[1]);
                    LocInfo li = GlobeDataStore.getInstance(mappath).findCityDirect(lat, lon);
                    if (li != null) {
                        Log.info(String.format("[%f, %f]->%s", lon, lat, li.toString()));
                        socket.write(li.toString());
                    } else {
                        socket.write("{}");
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    socket.write("{}");
                }
            });
        });
        server.listen(port, "localhost");
    }
}
