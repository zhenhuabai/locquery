package common;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import locutil.UserLocation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * Created by 白振华 on 2017/2/16.
 */
@RunWith(VertxUnitRunner.class)
public class MongoDbHelperTest {
    MongoClient client = null;
    private long UID = 1001001;
    private Vertx vertx;
    @Before
    public void setUp() throws Exception {
        Config.enableLog();
        Config.enableDebug(true);
        vertx = Vertx.vertx();
        client = MongoDbHelper.getInstance().requestClient(vertx);
    }

    @After
    public void tearDown(TestContext context) throws Exception {
        client.close();
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void putUserLocation(TestContext context) throws Exception {
        final Async async = context.async();
        UserLocation ul = new UserLocation(UID,38.2,108.33,System.currentTimeMillis());
        MongoDbHelper.putUserLocation(client,ul, res->{
            if(res.succeeded()) {
                context.assertTrue(!res.result().isEmpty());
                async.complete();
            } else {
                context.assertTrue(false);
                async.complete();
            }
        });
    }

    @Test
    public void setUserLocal(TestContext context) throws Exception {
        final Async async = context.async();
        UserLocation ul = new UserLocation(UID,38.2,108.33,System.currentTimeMillis());
        MongoDbHelper.setAnalyzedLocal(client,ul.toJsonObject(), res->{
            if(res.succeeded()) {
                context.assertTrue(!res.result().isEmpty());
                async.complete();
            } else {
                context.assertTrue(false);
                async.complete();
            }
        });
    }

    @Test
    public void setAnalyzedLocal(TestContext context) throws Exception {
        final Async async = context.async();
        //String json = "{\"result\":[{\"cityinfo\":{\"en\":\"{\"country\":\"China\",\"province\":\"Jiangsu\",\"city\":\"Nanjing\",\"county\":\"Nanjing\",\"lang\":\"en\"}\",\"zh\":\"{\"country\":\"China\",\"province\":\"Jiangsu\",\"city\":\"Nanjing\",\"county\":\"Nanjing\",\"lang\":\"en\"}\"},\"probability\":1.0,\"count\":10}],\"timestamp\":1487695079043,\"total_records\":10,\"uid\":100001,\"_id\":\"58ac6ce76168900830699ef5\"}";
        //String json = "{\"result\":[{\"cityinfo\":{\"en\":\"{\"country\":\"China\",\"province\":\"Jiangsu\",\"city\":\"Nanjing\",\"county\":\"Nanjing\",\"lang\":\"en\"}\",\"zh\":\"{\"country\":\"China\",\"province\":\"Jiangsu\",\"city\":\"Nanjing\",\"county\":\"Nanjing\",\"lang\":\"en\"}\"},\"probability\":1.0,\"count\":10}],\"timestamp\":1487695079043,\"total_records\":10,\"uid\":100001}";

        //String json = "{\"result\":[{\"cityinfo\":{\"en\":\"{\"country\":\"China\",\"province\":\"Jiangsu\",\"city\":\"Nanjing\",\"county\":\"Nanjing\",\"lang\":\"en\"}\",\"zh\":\"{\"country\":\"China\",\"province\":\"Jiangsu\",\"city\":\"Nanjing\",\"county\":\"Nanjing\",\"lang\":\"en\"}\"},\"probability\":1.0,\"count\":10}],\"timestamp\":1487695079043,\"total_records\":10,\"uid\":100001,\"_id\":\"58ac6ce76168900830699ef5\"}";
//                String json = "{\"result\":[\"cityinfo\":{\"en\":\"{\"country\":\"China\",\"province\":\"Jiangsu\",\"city\":\"Nanjing\",\"county\":\"Nanjing\",\"lang\":\"en\"}\",\"probability\":1.0,\"count\":10}],\"timestamp\":1487695079043,\"total_records\":10,\"uid\":100001}";

        String json = "{\"result\":[{\"cityinfo\":{\"en\":\"{\"country\":\"China\",\"province\":\"Jiangsu\",\"city\":\"Nanjing\",\"county\":\"Nanjing\",\"lang\":\"en\"}\",\"zh\":\"{\"country\":\"China\",\"province\":\"Jiangsu\",\"city\":\"Nanjing\",\"county\":\"Nanjing\",\"lang\":\"en\"}\"},\"probability\":1.0,\"count\":10}],\"timestamp\":1487695079043,\"total_records\":10,\"uid\":100001}";


        //String json = "{\"result\":[{\"cityinfo\":{\"en\":33},\"probability\":1.0,\"count\":10}],\"timestamp\":1487695079043,\"total_records\":10,\"uid\":100001}";
        JsonObject toStore = new JsonObject(json);
        System.out.println("saving:"+toStore.toString());
        MongoDbHelper.setAnalyzedLocal(client,toStore, res->{
            if(res.succeeded()) {
                context.assertTrue(!res.result().isEmpty());
                async.complete();
            } else {
                context.assertTrue(false);
                async.complete();
            }
        });
    }
}