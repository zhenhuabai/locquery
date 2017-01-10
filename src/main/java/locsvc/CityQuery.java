package locsvc;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import common.LocInfo;
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
        String[] result = null;
        LocInfo li = GlobeDataStore.getInstance().findCityDirect(lat, lon);
        if(li != null){
            result = li.values();
        }
        return result;
    }
}
