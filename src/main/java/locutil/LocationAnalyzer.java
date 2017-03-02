package locutil;

import com.huleibo.LocationManager;
import common.Config;
import common.MongoDbHelper;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import javax.print.attribute.standard.JobSheets;
import java.util.DoubleSummaryStatistics;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by 白振华 on 2017/2/20.
 * This utility class calculates the local cities from their occurrence in the location history
 * The final result is stored in MongoDbHelper.COLLECTION_USERLOCALANALYZED
 * the process is:
 * 1. retrieving the list of the city by user id, in the history table
 * 2. counting the total number of the city
 * 3. counting the total number of each city
 * 4. calculating the percentage of each city in the total
 * 5. if the percentage of a city meets the criteria, store it in analyzed table
 */
public final class LocationAnalyzer {
    private static final Logger logger = LogManager.getLogger(LocationAnalyzer.class);
    private static boolean localLocationStarted = false;
    private static LocationAnalyzer instance = new LocationAnalyzer();
    public static LocationAnalyzer getInstance(){return instance;};
    private MongoClient client;
    private long dayMillis = 24 * 60 * 60 * 1000;
    private long pastDays = 90;//TODO: read config
    private final long DEFAULT_PASTDAYS = 30;
    private double threshold_precentage = 0.0;//TODO: read config
    private static final int MILILIESINMINUTE = 60 * 1000;
    private int scaninterval = 10 * MILILIESINMINUTE;
    private static final String KEY_SCAN = "scaninterval";
    private static final String KEY_ANALYZER = "locationanalyzer";
    private static final String KEY_THRESHOLD = "thresholdpercentage";
    private static final String KEY_PASTDAYS = "pastdays";
    private LocationAnalyzer(){
        JSONObject jo = Config.getInstance().getLocationManagerConfig();
        JSONObject lz = (JSONObject)jo.get(LocationAnalyzer.KEY_ANALYZER);
        pastDays = DEFAULT_PASTDAYS;
        try {
            pastDays = Integer.parseInt(lz.get(LocationAnalyzer.KEY_PASTDAYS).toString());
            if (pastDays < 0) {
                pastDays = DEFAULT_PASTDAYS;
            }
        }catch (NumberFormatException e){
        }
        threshold_precentage = 0;
        if(lz.containsKey(LocationAnalyzer.KEY_THRESHOLD)){
            String threasholdS = lz.get(LocationAnalyzer.KEY_THRESHOLD).toString();
            //0 is allowed, which lets the city with minimal occurrence appear in local list
            try{
                threshold_precentage = Double.parseDouble(threasholdS);
                if (threshold_precentage < 0 || threshold_precentage > 1) {
                    threshold_precentage = 0;
                }
            }catch (NumberFormatException e){
            }
        }
        if(lz.containsKey(LocationAnalyzer.KEY_SCAN)){
            String interval = lz.get(LocationAnalyzer.KEY_SCAN).toString();
            //0 is allowed, which lets the city with minimal occurrence appear in local list
            try{
                scaninterval = Integer.parseInt(interval);
                if (scaninterval < 1) {
                    scaninterval = 1;
                }
                scaninterval *= MILILIESINMINUTE;
            }catch (NumberFormatException e){
            }
        }
        logger.info("initializing location analyzer with threshold = "+threshold_precentage+ " and days="+pastDays
                +" interval="+scaninterval/MILILIESINMINUTE+ "/minute");
    }
    private Timer localLocationTimer = new Timer("LocalLocation_Analyzer");
    private static long LOCALLOCATIONANALYZE_INTERVAL = 10 * 60 * 1000;//10 minutes should be fine
    public final void startLocalLocation(MongoClient client){
        LocationAnalyzer.getInstance().startLocalLocationAnalyzer(client,
                scaninterval);
    }
    private final void startLocalLocationAnalyzer(MongoClient client, long period){
        this.client = client;
        if(!localLocationStarted){
            localLocationStarted = true;
            LocalLocationAnalyzer lla = new LocalLocationAnalyzer();
            localLocationTimer.schedule(lla,0, period);
        }
    }
    public static final void stopLocalLocation() {
        LocationAnalyzer.getInstance().stopLocalLocationAnalyzer();
    }
    private final void stopLocalLocationAnalyzer(){
        if(localLocationStarted){
            localLocationStarted = false;
            localLocationTimer.cancel();
        }
    }

    private void parseUserLocal(long uid, Handler<AsyncResult<JsonObject>>handler){
        parseUserLocal(uid, threshold_precentage, client, handler);
    }
    //scan location history and find the qualified "local cities" with the
    //criteria of the probability threshold
    public void parseUserLocal(long uid, double threshold, MongoClient mongoClient, Handler<AsyncResult<JsonObject>>handler){
        long current = System.currentTimeMillis();
        long startday = current - dayMillis * pastDays;
        JsonObject mongocmd = new JsonObject();
        //String pipe = "[{$match:{$and:[{timestamp:{$gte:%d,%lte,%ld}}]}, {$group:{_id:{cityinfo:\"$cityinfo\"},count:{$sum:1}}}]";

        String pipe = "[{\"$match\": {\"$and\": [{\"uid\":{\"$eq\":%d}}, {\"timestamp\":{\"$gte\":%d,\"$lte\":%d}}]}},"+
                "{\"$group\": {\"_id\":{\"cityinfo\":\"$cityinfo\"},\"count\":{\"$sum\":1}}}])";
        String mongoPipe = String.format(pipe,uid,startday,current);
        logger.debug("mango pipe string:"+mongoPipe);
        JsonArray mongopl = new JsonArray(mongoPipe);
        mongocmd.put("aggregate",MongoDbHelper.COLLECTION_USERLOCATION)
        .put("pipeline", mongopl);
        logger.debug("mangoClienet =:"+mongoClient);
        mongoClient.runCommand("aggregate",mongocmd,result->{
            //logger.debug("mango pipe result:"+result.result().toString());
            if(result.succeeded()) {
                logger.debug("mongo aggregate result:"+result.result().toString());
                JsonArray ja = result.result().getJsonArray("result");
                logger.debug("mongo aggregate result pretty:"+ja.encodePrettily());
                int total = 0;
                int[] occurance = new int[ja.size()];
                for (int j = 0; j < ja.size(); j++) {
                    JsonObject item = ja.getJsonObject(j);
                    JsonObject cityinfo = item.getJsonObject("_id").getJsonObject("cityinfo");
                    if(cityinfo != null) {
                        //document $cityinfo was string value from MapServer
                        if(cityinfo.isEmpty()){
                            logger.error("There are empty cityinfo in history!!!");
                        }else {
                            JsonObject en = new JsonObject(cityinfo.getString("en"));
                            String cityName = en.getString("city");
                        }
                        Integer number = item.getInteger("count");
                        occurance[j] = number;
                        total += number;
                    }
                }
                JsonArray returned = new JsonArray();
                Double per = 0.0;
                for (int i = 0; i < occurance.length; i++) {
                    //TODO:why float cause problem?
                    per =  (double)occurance[i] / total;
                    if (per >= threshold) {
                        JsonObject reorg = new JsonObject();
                        JsonObject cityinfo = ja.getJsonObject(i).getJsonObject("_id").getJsonObject("cityinfo");
                        Integer citycounter = ja.getJsonObject(i).getInteger("count");
                        reorg.put("cityinfo",cityinfo);
                        reorg.put("probability",per);
                        reorg.put("count",citycounter);
                        //returned.add(ja.getJsonObject(i).put("probability",per));
                        returned.add(reorg);
                    }
                }
                JsonObject ret = new JsonObject();
                ret.put("result",returned);
                ret.put("timestamp",current);
                ret.put("total_records",total);
                ret.put("uid",uid);
                logger.info("found city:" + ret.toString() + " with:"+per * 100+"%");
                Future<JsonObject> future = Future.future();
                future.complete(ret);
                handler.handle(future);
            }else {
                logger.error("problem querying history locations:"+result.cause().toString());
                Future<JsonObject> future = Future.failedFuture("Problem querying history");
                handler.handle(future);
            }
        });
    }
    //this is for a convenient UT
    public void execute(){
        client.distinct(MongoDbHelper.COLLECTION_USERLOCATION, UserLocation.UID,
                Long.class.getName(), result->{
                    logger.info(String.format("Analyzing local cities from location history[%d,%f]...",
                            pastDays,threshold_precentage));
                    if(result.succeeded()) {
                        JsonArray userids = result.result();
                        userids.forEach(o -> {
                            Long uid = (Long) o;
                            parseUserLocal(uid, res -> {
                                if (res.succeeded()) {
                                    MongoDbHelper.setAnalyzedLocal(client, res.result(), dbResult -> {
                                        if (dbResult.succeeded()) {
                                            logger.debug("updated analyzed result:" + uid);
                                        } else {
                                            logger.error("problem saving analyzed result:" + dbResult.toString());
                                        }
                                    });
                                    JsonArray ja = res.result().getJsonArray("result");
                                    ja.forEach(jsonObj -> {
                                        JsonObject joItem = (JsonObject) jsonObj;
                                        JsonObject cityinfo = joItem.getJsonObject("cityinfo");
                                        JsonObject en = new JsonObject(cityinfo.getString("en"));
                                        String cityName = en.getString("city");
                                        Integer number = joItem.getInteger("count");
                                        Float prob = joItem.getFloat("probability");
                                        logger.info(String.format("\t\tAnalyzed city: [uid,city,number,probability]->[%d,%s,%d,%f]",
                                                uid, cityName, number, prob));
                                    });
                                } else {
                                    logger.error("error processing uid=" + uid);
                                }
                            });
                        });
                        logger.debug("ids:" + userids.encodePrettily());
                    }else{
                        logger.error(result.cause().toString());
                    }
                });
    }
    class LocalLocationAnalyzer extends TimerTask{
        public LocalLocationAnalyzer(){
        }
        @Override
        public void run() {
            execute();
        }
    }
}
