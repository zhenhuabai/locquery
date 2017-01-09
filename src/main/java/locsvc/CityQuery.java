package locsvc;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import locutil.GlobeDataStore;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by 白振华 on 2017/1/7.
 */
public class CityQuery {
    private final static Logger Log = Logger.getLogger("CityQuery");
    public static String[] find(double lat, double lon) throws Exception{
        long start = System.currentTimeMillis();
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory( null );
        Coordinate coord = new Coordinate( lat, lon );
        Point point = geometryFactory.createPoint( coord );
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2( null );
        Filter filter = ff.contains( ff.property( "the_geom" ), ff.literal( point ) );
        Query query = new Query(GlobeDataStore.getInstance().store.getTypeNames()[0], filter,
                new String[] { "ID_0", "ID_1", "ID_2", "ID_3", "NAME_0", "NAME_1", "NAME_2", "NAME_3", "ENGTYPE_3", "VARNAME_3", "NL_NAME_3"});
        //Query query = new Query(GlobeDataStore.getInstance().store.getTypeNames()[0], filter);
        SimpleFeatureCollection features = GlobeDataStore.getInstance().featureSource.getFeatures(query);
        long end = System.currentTimeMillis();
        Log.info(String.format("[%f, %f] query takes: %dms", lat, lon, end - start));
        if (features.size() < 1){
            throw new Exception("City not found");
        } else {
            if (features.size() > 1) {
                Log.warning("Wrong map boundary");
            }
            String[] result = null;
            try ( SimpleFeatureIterator iterator = features.features() ) {
                while (iterator.hasNext()) {
                    SimpleFeature feature = iterator.next();
                    String ln = feature.getAttribute("NL_NAME_3").toString();
                    System.out.print("ln = "+ln);
                    result = new String[] {
                            feature.getAttribute("NAME_0").toString(),
                            feature.getAttribute("NAME_1").toString(),
                            feature.getAttribute("NAME_2").toString(),
                            feature.getAttribute("NAME_3").toString(),
                            feature.getAttribute("ID_0").toString(),
                            feature.getAttribute("ID_1").toString(),
                            feature.getAttribute("ID_2").toString(),
                            feature.getAttribute("ID_3").toString()
                    };
                }
            }
            return result;
        }
    }
}
