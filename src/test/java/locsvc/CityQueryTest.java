package locsvc;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by 白振华 on 2017/1/7.
 */
public class CityQueryTest {
    @Test
    public void find() throws Exception {
        try {
            String[] res = CityQuery.find(118.848166, 32.40064);
            assertEquals(res[0].toString(), "China");
            assertEquals(res[1].toString(), "Jiangsu");
            assertEquals(res[2].toString(), "Nanjing");
            //assertEquals(res[3].toString(), "六合区");
            res = CityQuery.find(109.594513,34.644989);
            assertEquals(res[0].toString(), "China");
            assertEquals(res[1].toString(), "Shaanxi");
            assertEquals(res[2].toString(), "Weinan");
            //assertEquals(res[3].toString(), "Weinan");
        }catch (Exception e){
            e.printStackTrace();
            System.out.print("error querying location");
        }
    }

}