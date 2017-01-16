package weatherutil;

/**
 * Created by 白振华 on 2017/1/14.
 */
public class WeatherDatabase {
    private static WeatherDatabase ourInstance = new WeatherDatabase();

    public static WeatherDatabase getInstance() {
        return ourInstance;
    }

    private WeatherDatabase() {
    }
}
