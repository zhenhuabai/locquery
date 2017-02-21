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
 */
public final class LocationAnalyzer {
    private static final Logger logger = LogManager.getLogger(LocationAnalyzer.class);
    private static boolean localLocationStarted = false;
    private static LocationAnalyzer instance = new LocationAnalyzer();
    public static LocationAnalyzer getInstance(){return instance;};
    private MongoClient client;
    private long dayMillis = 24 * 60 * 60 * 1000;
    private long pastDays = 90;//TODO: read config
    private double threshold_precentage = 0.4;//TODO: read config
    private LocationAnalyzer(){
        JSONObject jo = Config.getInstance().getLocationManagerConfig();
        JSONObject lz = (JSONObject)jo.get("locationanalyzer");
        pastDays = Integer.parseInt(lz.get("pastdays").toString());
        threshold_precentage = Double.parseDouble(lz.get("thresholdpercentage").toString());
        logger.info("initializing location analyzer with threshold = "+threshold_precentage+ " and days="+pastDays );
    }
    private Timer localLocationTimer = new Timer("LocalLocation_Analyzer");
    private final static long LOCALLOCATIONANALYZE_INTERVAL = 10 * 60 * 1000;//10 minutes should be fine
    public static final void startLocalLocation(MongoClient client){
        LocationAnalyzer.getInstance().startLocalLocationAnalyzer(client,
                LOCALLOCATIONANALYZE_INTERVAL);
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
        client.runCommand("aggregate",mongocmd,result->{
            //logger.debug("mango pipe result:"+result.result().toString());
            if(result.succeeded()) {
                JsonArray ja = result.result().getJsonArray("result");
                //logger.debug("mongo aggregate result:"+ja.encodePrettily());
                int total = 0;
                int[] occurance = new int[ja.size()];
                for (int j = 0; j < ja.size(); j++) {
                    JsonObject item = ja.getJsonObject(j);
                    JsonObject cityinfo = item.getJsonObject("_id").getJsonObject("cityinfo");
                    if(cityinfo != null) {
                        //document $cityinfo was expanded to string value in query
                        JsonObject en = new JsonObject(cityinfo.getString("en"));
                        String cityName = en.getString("city");
                        Integer number = item.getInteger("count");
                        occurance[j] = number;
                        total += number;
                    }
                }
                JsonArray returned = new JsonArray();
                JsonObject reorg = new JsonObject();
                for (int i = 0; i < occurance.length; i++) {
                    //TODO:why float cause problem?
                    Double per = (Double) ((double)occurance[i] / total);
                    if (per >= threshold_precentage) {
                        reorg.clear();
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
                logger.info("found city:" + ret.toString());
                Future<JsonObject> future = Future.future();
                future.complete(ret);
                handler.handle(future);
            }else {
                handler.handle(result);
            }
        });
    }
    //this is for a convenient UT
    public void execute(){
        client.distinct(MongoDbHelper.COLLECTION_USERLOCATION, UserLocation.UID,
                Long.class.getName(), result->{
                    JsonArray userids = result.result();
                    userids.forEach(o->{
                        Long uid = (Long)o;
                        parseUserLocal(uid, res->{
                            if(res.succeeded()){
                                MongoDbHelper.setAnalyzedLocal(client,res.result(), dbResult->{
                                    if(dbResult.succeeded()){
                                        logger.debug("updated analyzed result:"+uid);
                                    }else{
                                        logger.error("problem saving analyzed result:"+dbResult.toString());
                                    }
                                });
                                JsonArray ja = res.result().getJsonArray("result");
                                ja.forEach(jsonObj->{
                                    JsonObject joItem = (JsonObject) jsonObj;
                                    JsonObject cityinfo = joItem.getJsonObject("cityinfo");
                                    JsonObject en = new JsonObject(cityinfo.getString("en"));
                                    //document $cityinfo was expanded to string value in query
                                    //JsonObject cityinfo = joItem.getJsonObject("_id").getJsonObject("cityinfo");
                                    //JsonObject en = new JsonObject(cityinfo.getString("en"));
                                    String cityName = en.getString("city");
                                    Integer number = joItem.getInteger("count");
                                    Float prob = joItem.getFloat("probability");
                                    logger.info(String.format("\t\tAnalyzed city: [uid,city,number,probability]->[%d,%s,%d,%f]",
                                            uid,cityName,number,prob));
                                });
                            }else{
                                logger.error("error processing uid="+uid);
                            }
                        });
                    });
                    logger.debug("ids:"+userids.encodePrettily());
                });
    }
    class LocalLocationAnalyzer extends TimerTask{
        public LocalLocationAnalyzer(){
        }
        @Override
        public void run() {
            //fetch uniq uids
            execute();
        }
    }
}
