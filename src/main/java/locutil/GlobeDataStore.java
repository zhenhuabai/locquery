package locutil;

import java.io.File;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import common.Config;
import common.Loggable;
import common.OsCheck;
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

/**
 * Created by 白振华 on 2017/1/7.
 */
public final class GlobeDataStore extends Loggable{
    private static GlobeDataStore instance = null;
    //windows test
    private String file = "c:\\gis\\CHN_adm\\CHN_adm0.shp";
    //private String file = "c:\\gis\\dpkg\\CHN_adm.gpkg";
    //unix like
    private String fileUnix = "/opt/locquery/data/CHN_adm3.shp";
    private File datafile = new File(file);
    public FileDataStore store = null;
    public SimpleFeatureSource featureSource = null;
    private GlobeDataStore(){
        super();
        String m = Config.getInstance().getConfig().get("http.port").toString();
        Log.info("config type is:"+m);
        Log.info("OS type is:"+OsCheck.getOperatingSystemType());
        System.out.print(OsCheck.getOperatingSystemType());
        if (OsCheck.getOperatingSystemType() == OsCheck.OSType.Linux) {
            datafile = new File(fileUnix);
        }
        loadGlobleData();
    };
    public static GlobeDataStore getInstance(){
        if (instance == null){
            instance = new GlobeDataStore();
        }
        return instance;
    }
    private void loadGlobleData(){
        try {
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

}
