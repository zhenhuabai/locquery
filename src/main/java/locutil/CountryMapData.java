package locutil;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import common.Loggable;
import org.geotools.data.DataStore;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

/**
 * Created by dbai on 10/01/2017.
 */
public class CountryMapData extends Loggable{
    DataStore outlineMap = null;
    DataStore detailMap = null;
    String name = null;
    String[] columns = null;
    CountryMapData(String name, DataStore out, DataStore detail, String[] cols){
        this.name = name;
        outlineMap = out;
        detailMap = detail;
        columns = cols;
    }
    boolean contains (double lat, double lon){
        long start = System.currentTimeMillis();
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory( null );
        Coordinate coord = new Coordinate( lat, lon );
        Point point = geometryFactory.createPoint( coord );
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2( null );
        Filter filter = ff.contains( ff.property( "the_geom" ), ff.literal( point ) );
        boolean yes = false;
        try {
            Query query = new Query(outlineMap.getTypeNames()[0], filter, new String[]{"ID_0"});
            SimpleFeatureCollection features = outlineMap.getFeatureSource(outlineMap.getTypeNames()[0]).getFeatures(query);
            if (features.size() > 0){
                Log.info(String.format("[%d, %d] found in %s\n",lat, lon, name));
                yes = true;
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        long end = System.currentTimeMillis();
        Log.info(String.format("[%f, %f] Outline query takes: %dms", lat, lon, end - start));
        return yes;
    }
    public LocInfo getCityDirect(double lat, double lon) {
        long start = System.currentTimeMillis();
        LocInfo retInfo = null;
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory( null );
        Coordinate coord = new Coordinate(lat, lon );
        Point point = geometryFactory.createPoint( coord );
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2( null );
        Filter filter = ff.contains( ff.property( "the_geom" ), ff.literal( point ) );
        SimpleFeatureCollection features;
        try {
            Query query = new Query(detailMap.getTypeNames()[0], filter,
                    columns);
                    //new String[] { "ID_0", "ID_1", "ID_2", "ID_3", "NAME_0", "NAME_1", "NAME_2", "NAME_3", "ENGTYPE_3", "VARNAME_3", "NL_NAME_3"});
            features = detailMap.getFeatureSource(detailMap.getTypeNames()[0]).getFeatures(query);
            if (features.size() < 1){
                Log.severe("Map data not consistent!");
            } else {
                if (features.size() > 1) {
                    Log.warning("Wrong map boundary");
                }
                try (SimpleFeatureIterator iterator = features.features()) {
                    while (iterator.hasNext()) {
                        SimpleFeature feature = iterator.next();
                        String[] result = new String[columns.length];
                        for (int i = 0; i < result.length; i++) {
                            result[i] = feature.getAttribute(columns[i]).toString();
                        }
                        retInfo = new LocInfo(result);
                        break; //Get the first
                    }
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        long end = System.currentTimeMillis();
        Log.info(String.format("[%f, %f] detail direct query takes: %dms", lat, lon, end - start));
        return retInfo;
    }
    public LocInfo getCity(double lat, double lon)
    {
        long start = System.currentTimeMillis();
        LocInfo retInfo = null;
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory( null );
        Coordinate coord = new Coordinate(lat, lon );
        Point point = geometryFactory.createPoint( coord );
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2( null );
        Filter filter = ff.contains( ff.property( "the_geom" ), ff.literal( point ) );
        SimpleFeatureCollection features;
        try {
            Query query = new Query(outlineMap.getTypeNames()[0], filter, new String[]{"NAME_ISO"});
            features = outlineMap.getFeatureSource(outlineMap.getTypeNames()[0]).getFeatures(query);
            if (features.size() > 0){
                Log.info(String.format("[%f, %f] found in map %s\n",lat, lon, name));
                query = new Query(detailMap.getTypeNames()[0], filter,
                        columns);
                features = detailMap.getFeatureSource(detailMap.getTypeNames()[0]).getFeatures(query);
                if (features.size() < 1){
                    Log.severe("Map data not consistent!");
                } else {
                    if (features.size() > 1) {
                        Log.warning("Wrong map boundary");
                    }
                    try (SimpleFeatureIterator iterator = features.features()) {
                        while (iterator.hasNext()) {
                            SimpleFeature feature = iterator.next();
                            String[] result = new String[columns.length];
                            for (int i = 0; i < result.length; i++) {
                                result[i] = feature.getAttribute(columns[i]).toString();
                            }
                            retInfo = new LocInfo(result);
                            break; //Get the first
                        }
                    }
                }
            } else {
                Log.info(String.format("[%d, %d] NOT found in map %s\n",lat, lon, name));
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        long end = System.currentTimeMillis();
        Log.info(String.format("[%f, %f] detail query takes: %dms", lat, lon, end - start));
        return retInfo;
    }
}
