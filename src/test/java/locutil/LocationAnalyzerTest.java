package locutil;

import common.Config;
import common.MongoDbHelper;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import io.vertx.ext.mongo.MongoClient;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Created by 白振华 on 2017/2/20.
 */
@RunWith(VertxUnitRunner.class)
public class LocationAnalyzerTest {
    private MongoClient client = null;
    private Vertx vertx;
    @Test
    public void startLocalLocation() throws Exception {
        LocationAnalyzer.startLocalLocation(client);
        //let's assume analyzer finishes in 5 seconds
        Thread.sleep(5*1000);
        LocationAnalyzer.stopLocalLocation();
    }
    @Before
    public void setUp() throws Exception {
        vertx = Vertx.vertx();
        Config.enableLog();
        Config.enableDebug(true);
        client = MongoDbHelper.getInstance().requestClient(vertx);
    }

    @After
    public void tearDown(TestContext context) throws Exception {
        client.close();
        vertx.close();
    }
}
