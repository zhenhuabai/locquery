package weatherutil;

import io.netty.handler.codec.http.HttpResponse;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientResponse;

/**
 * Created by 白振华 on 2017/1/14.
 */
public abstract class WeatherSource {
    public String name;
    protected Vertx vertx;
    public int minutelyLimit;//permitted calls per minute
    public int hourlyLimit;//permitted calls per minute
    public int dailyLimit;//permitted calls per minute
    public int updateInterval;//in minute,interval for the source update its data
    private long lastupdatetime;//last api call to get data
    protected String queryURL; //url to query from the source
    protected String queryTemplate; //url to query from the source
    private boolean locSupported;//if geolocation based query supported
    public WeatherSource(){
       vertx = Vertx.vertx();
       System.out.println("WeatherSource");
    }
    public abstract boolean isAvailble();
    public abstract void getData(double lon, double lat,
                                 Handler<HttpClientResponse> handler);
    public abstract void getData(String cityname,
                                 Handler<HttpClientResponse> handler);
}
