package locutil;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import common.Config;
import common.LocInfo;
import common.Loggable;
import common.OsCheck;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.data.JFileDataStoreChooser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Created by 白振华 on 2017/1/7.
 */
public final class GlobeDataStore extends Loggable{
    private static GlobeDataStore instance = null;
    //windows test
    //private String file = "c:\\gis\\CHN_adm\\CHN_adm0.shp";
    //private String file = "c:\\gis\\dpkg\\CHN_adm.gpkg";
    //unix like
    //private String fileUnix = "/opt/locquery/data/CHN_adm3.shp";
    //private File datafile = new File(file);
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
            Log.info(String.format("Loading %s [%s],[%s]\n", name, path1, path2));
            outline = loadDataStore(path1);
            detail = loadDataStore(path2);
            loadedMap.add(new CountryMapData(name, outline, detail));
            Log.info(String.format("Map %s installed\n", name));
        }
        long end = System.currentTimeMillis();
        Log.info(String.format("%d seconds took loading map data\n", (end - start)/1000));
    };
    public static GlobeDataStore getInstance(){
        if (instance == null){
            instance = new GlobeDataStore();
        }
        return instance;
    }
    private DataStore loadDataStore(String filename){
        store = null;
        try {
            File file = new File(filename);
            Map<String, Object> map = new HashMap<>();
            map.put("url", file.toURI().toURL());
            map.put("charset", "utf-8");
            store = DataStoreFinder.getDataStore(map);
            Log.info("Loaded the map data:"+filename);
        }catch (Exception e){
            e.printStackTrace();
            Log.severe("Error open database:"+filename);
        }
        return store;
    }
    /*
    private void loadGlobleData(){
        try {
            maps = Config.getInstance().getMapConfig();
            Map<String, Object> map = new HashMap<>();
            map.put("url", datafile.toURI().toURL());
            map.put("charset", "unico");
            store = FileDataStoreFinder.getDataStore(datafile);
            featureSource = store.getFeatureSource();
            Log.info("loaded the map data");
        }catch (Exception e){
            e.printStackTrace();
           Log.severe("Error open database");
        }
    }
    */
    public LocInfo findCityDirect(double lat, double lon){
        long start = System.currentTimeMillis();
        LocInfo loc = null;
        for(  CountryMapData d : loadedMap){
            loc = d.getCityDirect(lat, lon);
            if (loc != null){
                break;
            }
        }
        long end = System.currentTimeMillis();
        Log.info(String.format("Direct searching [%f, %f] took %d seconds\n", lat, lon, end - start));
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
        Log.info(String.format("Searching [%f, %f] took %d seconds\n", lat, lon, end - start));
        return loc;
    }

}
