package weatherutil;

/**
 * Created by 白振华 on 2017/1/14.
 */
public class WeatherSource {
    public String name;
    public int minutelyLimit;//permitted calls per minute
    public int hourlyLimit;//permitted calls per minute
    public int dailyLimit;//permitted calls per minute
    public int updateInterval;//in minute,interval for the source update its data
    private long lastupdatetime;//last api call to get data
    protected String queryURL; //url to query from the source
    private boolean locSupported;//if geolocation based query supported
    public boolean isAvailble(){
        boolean ret = true;

        return ret;
    }
    public WeatherData getData(double lon, double lat){
        WeatherData wd = new WeatherData();
        return wd;
    }

}
