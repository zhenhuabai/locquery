package locsvc;

import common.Config;
import io.vertx.ext.unit.TestContext;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by 白振华 on 2017/1/7.
 */
public class CityQueryTest {
    @Before
    public void setUp() {
        Config.enableLog();
    }
    @Test
    public void find() throws Exception {
        try {
            String[] res = CityQuery.find(118.848166, 32.40064);
            boolean cn = res[0].toString().matches("China|中国");
            boolean js = res[1].toString().matches("Jiangsu|江苏省");
            boolean nj = res[2].toString().matches("Nanjing|南京市");
            boolean lh = res[3].toString().matches("Luhe|六合");
            assertTrue(cn);
            assertTrue(js);
            assertTrue(lh);
            assertTrue(nj);
            res = CityQuery.find(109.594513,34.644989);
            cn = res[0].toString().matches("China|中国");
            js = res[1].toString().matches("Shaanxi|陕西省");
            nj = res[2].toString().matches("Weinan|渭南市");
            lh = res[3].toString().matches("Weinan|渭南");
            assertTrue(cn);
            assertTrue(js);
            assertTrue(lh);
            assertTrue(nj);
        }catch (Exception e){
            e.printStackTrace();
            System.out.print("error querying location");
        }
    }

}