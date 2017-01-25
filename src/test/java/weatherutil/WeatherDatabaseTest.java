package weatherutil;

import common.Config;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by 白振华 on 2017/1/20.
 */
public class WeatherDatabaseTest {

    @Before
    public void setUp() {
        Config.enableLog();
        WeatherDatabase.debug = true;
    }
    @Test
    public void getWeatherDatabyCached() throws Exception {
        WeatherData wd = WeatherDatabase.getInstance().getWeatherData("China",
                "Shanxi","Taiyuan","");
        System.out.println("temperature: "+wd.data.get("temperature"));
        System.out.println("weather: "+wd.data.get("weather"));
        System.out.println("updated:"+wd.data.get("update"));
        assertEquals(wd.data.get("name"), "太原");
    }
    @Test
    public void csvFileReader() throws Exception {
        String file = "temp.csv";
        HashMap<String, WeatherData> wds = WeatherDatabase.getInstance().readWeatherData(file);
        WeatherData wd = wds.get("jiangsu,nanjing");
        assertEquals(wd.data.get("name"), "南京");
        assertEquals(wd.data.get("temperature"), "2");
        assertEquals(wd.data.get("weather"), "晴");
        assertEquals(wd.data.get("update"), "2017-01-20T22:40:00+08:00");
    }
    @Test
    public void csvFileWrite() throws Exception {
        String file = "temp.csv";
        WeatherData wd = new WeatherData();
        wd.data.put("name", "南京");
        wd.data.put("weather", "晴");
        wd.data.put("temperature", "2");
        wd.data.put("update", "2017-01-20T22:40:00+08:00");
        HashMap<String, WeatherData> wds = new HashMap<String, WeatherData>();
        wds.put("jiangsu,nanjing",wd);
        WeatherDatabase.getInstance().writeWeatherData(file, wds);
    }
    @Test
    public void testRefresh() throws Exception {
        WeatherDatabase.getInstance().setCityRefreshInterval(50*1000);
        WeatherDatabase.getInstance().setDbRefreshInterval(20*1000);
    }
    @Test
    public void getInstance() throws Exception {
        while(!WeatherDatabase.getInstance().isSyncing()) {
            Thread.sleep(1000);
        }
        while(WeatherDatabase.getInstance().isSyncing()) {
            Thread.sleep(1000);
        }
        System.out.println("Weather test complete!");
        /*
        new Thread(){
            @Override
            public void run() {
                super.run();
                WeatherDatabase.getInstance();
            }
        }.start();
        while (!WeatherDatabase.getInstance().isSyncing()){
           System.out.println("Let's wait for sync start");
           Thread.sleep(1000);
        }
        while (WeatherDatabase.getInstance().isSyncing()){
            counter++;
            System.out.println("Let's wait for sync complete..."+counter);
            Thread.sleep(1000);
        }
        */
    }

    @Test
    public void getWeatherData() throws Exception {
        String city = "akesu";
        WeatherData wd = WeatherDatabase.getInstance().getWeatherData(city);
        System.out.println("wd="+wd.toString());
        //wd = WeatherDatabase.getInstance().getWeatherData(city);
        assertTrue(wd.toString().contains("temperature"));
        System.out.println("city="+wd.toString());
        city = "xinjiang akesu";
        wd = WeatherDatabase.getInstance().getWeatherData(city);
        assertTrue(wd.toString().contains("temperature"));
        city = "xjing";
        wd = WeatherDatabase.getInstance().getWeatherData(city);
        System.out.println("city="+wd.toString());
        city = "xinjiang akesu";
        assertTrue(wd.toString().contains("not found"));
        city = "Beijing";
        wd = WeatherDatabase.getInstance().getWeatherData(city);
        System.out.println("city="+wd.toString());
        assertTrue(wd.toString().contains("temperature"));
        city = "weinan";
        wd = WeatherDatabase.getInstance().getWeatherData(city);
        System.out.println("city="+wd.toString());
        assertTrue(wd.toString().contains("temperature"));
        city = "Weinan";
        wd = WeatherDatabase.getInstance().getWeatherData(city);
        System.out.println("city="+wd.toString());
        assertTrue(wd.toString().contains("temperature"));
        city = "Shaanxi Weinan";
        wd = WeatherDatabase.getInstance().getWeatherData(city);
        assertTrue(wd.toString().contains("temperature"));
        System.out.println("city="+wd.toString());
        city = "xxWeinan";
        wd = WeatherDatabase.getInstance().getWeatherData(city);
        assertTrue(wd.toString().contains("city not found"));
        System.out.println("city="+wd.toString());
    }

}