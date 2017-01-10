package locutil;

import common.LocInfo;
import org.junit.Test;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import static org.junit.Assert.*;

/**
 * Created by dbai on 10/01/2017.
 */
public class GlobeDataStoreTest {
    @Test
    public void getInstance() throws Exception {
        GlobeDataStore.getInstance();
    }
    @Test
    public void findCity(){
        long start = System.currentTimeMillis();
        GlobeDataStore.getInstance().findCity(118.848166, 32.40064);
        GlobeDataStore.getInstance().findCity(109.594513,34.644989);
        long time = System.currentTimeMillis() - start;
        System.out.println("Takes "+time);
    }

    @Test
    public void findCityDirect(){
        long start = System.currentTimeMillis();
        GlobeDataStore.getInstance().findCityDirect(118.848166, 32.40064);
        GlobeDataStore.getInstance().findCityDirect(109.594513,34.644989);
        long time = System.currentTimeMillis() - start;
        System.out.println("Takes "+time);
    }
}