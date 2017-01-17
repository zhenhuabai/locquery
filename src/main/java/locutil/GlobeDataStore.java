package locutil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import common.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Created by 白振华 on 2017/1/7.
 */
public final class GlobeDataStore {
    private static final Logger logger = LogManager.getLogger(GlobeDataStore.class);
    private static GlobeDataStore instance = null;
    public DataStore store = null;
    public SimpleFeatureSource featureSource = null;
    private JSONArray maps = null;
    private ArrayList<CountryMapData> loadedMap = new ArrayList<CountryMapData>();

    private GlobeDataStore(){
        super();
        DataStore outline, detail;
        long start = System.currentTimeMillis();
        JSONArray maps = Config.getInstance().getMapConfig();
        for (int i = 0; i < maps.size(); i++) {
            JSONObject map = (JSONObject)maps.get(i);
            String name = map.get("name").toString();
            String path1 = map.get("outline").toString();
            String path2 = map.get("detail").toString();
            JSONArray jsobj = (JSONArray)map.get("columns");
            String[] cols = new String[jsobj.size()];
            jsobj.toArray(cols);
            logger.info("json="+jsobj.toString());
            //String[] cols = {"name_0","name_1"};//(JSONArray)map.get("columns");
            logger.info(String.format("Loading %s [%s],[%s]\n", name, path1, path2));
            outline = loadDataStore(path1);
            detail = loadDataStore(path2);
            loadedMap.add(new CountryMapData(name, outline, detail, cols));
            logger.info(String.format("Map %s installed\n", name));
        }
        long end = System.currentTimeMillis();
        logger.info(String.format("%d seconds took loading map data\n", (end - start)/1000));
    };
    public static GlobeDataStore getInstance(){
        if (instance == null){
            instance = new GlobeDataStore();
        }
        return instance;
    }
    public static GlobeDataStore getInstance(String mapname){
        if (instance == null){
            instance = new GlobeDataStore(mapname);
        }
        return instance;
    }
    private GlobeDataStore(String storename){
        super();
        logger.info("Single map service mode");
        DataStore outline, detail;
        long start = System.currentTimeMillis();
        JSONArray maps = Config.getInstance().getMapConfig();
        for (int i = 0; i < maps.size(); i++) {
            JSONObject map = (JSONObject)maps.get(i);
            String name = map.get("name").toString();
            if(name.equals(storename)) {
                String path1 = map.get("outline").toString();
                String path2 = map.get("detail").toString();
                JSONArray jsobj = (JSONArray) map.get("columns");
                String[] cols = new String[jsobj.size()];
                jsobj.toArray(cols);
                logger.info(String.format("Loading %s [%s],[%s]\n", name, path1, path2));
                outline = loadDataStore(path1);
                detail = loadDataStore(path2);
                loadedMap.add(new CountryMapData(name, outline, detail, cols));
                logger.info(String.format("Map %s installed\n", name));
                break;
            }
        }
        long end = System.currentTimeMillis();
        logger.info(String.format("%d seconds took loading map data\n", (end - start)/1000));
    }
    private DataStore loadDataStore(String filename){
        store = null;
        try {
            File file = new File(filename);
            Map<String, Object> map = new HashMap<>();
            map.put("url", file.toURI().toURL());
            map.put("charset", "iso-8859-9");
            store = DataStoreFinder.getDataStore(map);
            logger.info("Loaded the map data:"+filename);
        }catch (Exception e){
            e.printStackTrace();
            logger.error("Error open database:"+filename);
        }
        return store;
    }
    public LocInfo findCityDirect(double lat, double lon){
        long start = System.currentTimeMillis();
        LocInfo loc = null;
        loc = loadedMap.get(0).getCityDirect(lat,lon);
        long end = System.currentTimeMillis();
        logger.info(String.format("Direct searching [%f, %f] took %d ms\n", lat, lon, end - start));
        return loc;
    }
    public LocInfo findCity(double lat, double lon){
        long start = System.currentTimeMillis();
        LocInfo loc = null;
        for(  CountryMapData d : loadedMap){
            loc = d.getCity(lat, lon);
            if (loc != null){
                break;
            }
        }
        long end = System.currentTimeMillis();
        logger.info(String.format("Searching [%f, %f] took %d ms\n", lat, lon, end - start));
        return loc;
    }

}
