package locutil;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import common.Loggable;
import common.OsCheck;
import org.geotools.data.*;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.data.JDataStoreWizard;
import org.geotools.swing.data.JFileDataStoreChooser;
import org.geotools.swing.wizard.JWizard;

import javax.swing.*;

/**
 * Created by 白振华 on 2017/1/7.
 */
public final class GlobeDataStore extends Loggable{
    private static GlobeDataStore instance = null;
    //windows test
    private String file = "c:\\gis\\CHN_adm\\CHN_adm3.shp";
    private String fileSpacialite = "c:\\gis\\gpkg\\CHN_adm.gpkg";
    //unix like
    private String fileUnix = "/opt/locquery/data/CHN_adm3.shp";
    private String fileSpacialiteUnix = "/opt/locquery/data/CHN_adm.gpkg";
    private File datafile = new File(file);
    public DataStore store = null;
    //public FileDataStore store = null;
    public SimpleFeatureSource featureSource = null;
    private GlobeDataStore(){
        super();
        Log.info("OS type is:"+OsCheck.getOperatingSystemType());
        if (OsCheck.getOperatingSystemType() == OsCheck.OSType.Linux) {
            datafile = new File(fileUnix);
            fileSpacialite = fileSpacialiteUnix;
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
            long start = System.currentTimeMillis();
            Map params = new HashMap();
            params.put("dbtype", "geopkg");
            params.put("database", fileSpacialite);
            store = DataStoreFinder.getDataStore(params);
            if (store == null){
                Log.severe("Problem openning database by "+fileSpacialite);
            }
            //store = FileDataStoreFinder.getDataStore(datafile);
            featureSource = store.getFeatureSource("CHN_adm3");
            long diff = System.currentTimeMillis() - start;
            Log.info("Loaded the map data with "+diff+"ms.");
            System.out.println("Loaded the map data with "+diff+"ms.");
        }catch (Exception e){
            e.printStackTrace();
           Log.severe("Error open database");
        }
    }

    public static void connectDatabase() {
        DataStore dataStore;
        JDataStoreWizard wizard = new JDataStoreWizard();
        try{
            final Iterator<DataStoreFactorySpi> availableDataStores = DataStoreFinder
					.getAvailableDataStores();
			while (availableDataStores.hasNext()) {
				final DataStoreFactorySpi nextDS = availableDataStores.next();
				System.out.println("available Datastore:"+nextDS.getDescription()+","
                +nextDS.getDisplayName());
				// LOGGER.debug("Available DataStores : "
				// + nextDS.getClass().toString());
			}
        } catch (Exception e){
            e.printStackTrace();
        }
        int result = wizard.showModalDialog();
        if (result == JWizard.FINISH) {
            Map<String, Object> connectionParameters = wizard.getConnectionParameters();
            connectionParameters.forEach((key,obj)->System.out.println("->"+key+":"+obj.toString()));
            try {
                dataStore = DataStoreFinder.getDataStore(connectionParameters);
                if (dataStore == null) {
                    JOptionPane.showMessageDialog(null, "Could not connect - check parameters");
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
