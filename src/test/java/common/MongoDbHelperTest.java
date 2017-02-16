package common;

import io.vertx.core.Vertx;
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
    private Vertx vertx;
    @Before
    public void setUp() throws Exception {
        Config.enableLog();
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
        UserLocation ul = new UserLocation(10001,38.2,108.33,System.currentTimeMillis());
        MongoDbHelper.putUserLocation(client,ul, res->{
            if(res.succeeded()) {
                context.assertTrue(true);
                async.complete();
            } else {
                context.assertTrue(false);
                async.complete();
            }
        });
    }

}