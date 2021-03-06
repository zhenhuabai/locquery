package locutil;

import common.Config;
import io.vertx.ext.unit.TestContext;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by dbai on 10/01/2017.
 */
public class GlobeDataStoreTest {
    @Before
    public void setUp() {
        Config.enableLog();
    }
    @Test
    public void getInstance() throws Exception {
        GlobeDataStore.getInstance();
    }
    @Test
    public void findCity(){
        Config.enableLog();
        long start = System.currentTimeMillis();
        LocInfo res = GlobeDataStore.getInstance().findCity(118.848166, 32.40064);
        res.print();
        GlobeDataStore.getInstance().findCity(109.594513,34.644989);
        res.print();
        long time = System.currentTimeMillis() - start;
        System.out.println("Takes "+time+" ms");
    }

    @Test
    public void findCityDirect(){
        long start = System.currentTimeMillis();
        LocInfo res = GlobeDataStore.getInstance().findCityDirect(118.848166, 32.40064);
        res.print();
        res = GlobeDataStore.getInstance().findCityDirect(109.594513,34.644989);
        res.print();
        long time = System.currentTimeMillis() - start;
        System.out.println("Takes "+time+" ms");
    }
}