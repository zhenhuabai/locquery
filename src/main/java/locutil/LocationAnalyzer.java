package locutil;

import com.huleibo.LocationManager;
import common.MongoDbHelper;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    private long threshold_precentage = 80;//TODO: read config
    private LocationAnalyzer(){}
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
        
    }
    class LocalLocationAnalyzer extends TimerTask{
        public LocalLocationAnalyzer(){
        }
        @Override
        public void run() {
            //fetch uniq uids
            client.distinct(MongoDbHelper.COLLECTION_USERLOCATION, UserLocation.UID,
                    Long.class.getName(), result->{
                        JsonArray userids = result.result();
                        logger.debug("ids:"+userids.encodePrettily());
                    });
        }
    }
}
