package weatherutil;

import common.Config;
import io.netty.util.TimerTask;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hsqldb.util.CSVWriter;

import java.io.*;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.concurrent.Exchanger;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Created by 白振华 on 2017/1/14.
 */
public class WeatherDatabase {
    public static boolean debug = false;
    private static final Logger logger = LogManager.getLogger(WeatherDatabase.class);
    private static WeatherDatabase ourInstance = new WeatherDatabase();
    private boolean syncing = false;
    public boolean isSyncing(){
        return syncing;
    }

    public static WeatherDatabase getInstance() {
        return ourInstance;
    }

    Timer weatherRefresher = new Timer("WeatherRefresher");
    private HashMap<String, WeatherData> weatherDatabase = new HashMap<>();
    private HashMap<String, String[]> supportedCities = new HashMap<>();

    private static final long HOUR = 3600 * 1000;
    private long dbRefreshInterval = 2 * HOUR;
    private long cityRefreshInterval = 4 * HOUR;
    private XinZhiTianQi xz;
    private int debugCounter = 0;
    private WeatherDatabase() {
        xz = new XinZhiTianQi();
        loadSyncInterval();
        loadSupportedCities();
        weatherDatabase = readWeatherData(Config.getInstance().getWeatherConfig().get("dbfile").toString());
        weatherRefresher.schedule(new java.util.TimerTask() {
              @Override
              public void run() {
                  Object o = new Object();
                  syncing = true;
                  logger.info("Weather database sync starting..."+new Date().toString());
                  supportedCities.forEach((key, city)->{
                      if(debug){
                          debugCounter++;
                          if(debugCounter >2){
                              syncing = false;
                              return;
                          }
                      }
                      while (!xz.isAvailable()) {
                          try {
                              Thread.sleep(1000);
                          } catch (Exception e) {
                              logger.error("Sleep Exception!!");
                          }
                      }
                      if (shouldUpdate(key)) {
                          String search = getSearchParams(city);
                          if (xz.isAvailable()) {
                              logger.debug("search:" + search);
                              xz.getData(search, response -> {
                                  response.handler(
                                          body -> {
                                              WeatherData wd = xz.getWeatherData(body.toString());
                                              handleWeatherData(key, wd);
                                              if(wd != null) {
                                                  logger.info(wd.toString());
                                              }else{
                                                  logger.error("Not found:"+search);
                                              }
                                              synchronized (o){
                                                  o.notify();
                                              }
                                          });
                              });
                              synchronized (o){
                                  try{
                                      o.wait();
                                  }catch (Exception e){
                                      logger.error("Exception unexpected");
                                  }
                              }
                              logger.debug("Waiting for completion...");
                          } else {
                              logger.warn("Source unavailable!");
                          }
                      }else{
                          logger.info(key+" update time not due");
                      }
                  });
                 writeWeatherData(Config.getInstance().getWeatherConfig()
                 .get("dbfile").toString(), weatherDatabase);
                 syncing = false;
                 logger.info("Weather database sync completed:"+ new Date().toString());
              }
          },1000, dbRefreshInterval);
    }
    public void setDbRefreshInterval(long dbRefreshInterval){
        this.dbRefreshInterval = dbRefreshInterval;
    }
    public void setCityRefreshInterval(long cityRefreshInterval){
        this.cityRefreshInterval = cityRefreshInterval;
    }

    private boolean shouldUpdate(String key){
        WeatherData wd = weatherDatabase.get(key);
        boolean should = wd == null;
        if(!should && wd != null) {
            if (wd.data.isEmpty()) {
                should = true;
            } else {
                String data = wd.data.get("last");
                if (data != null && !data.isEmpty()) {
                    long last = Long.valueOf(data);
                    long current = System.currentTimeMillis();
                    should = (current - last) > cityRefreshInterval ;
                } else {
                    should = true;
                }
            }
        }
        return should;
    }
    private void handleWeatherData(String key, WeatherData wd){
        if(wd != null){
            //insert time stamp
            long tick = System.currentTimeMillis();
            String ticks = String.valueOf(tick);
            if(wd.data.containsKey("last")){
                wd.data.remove("last");
            }
            wd.data.put("last",ticks);
            if(weatherDatabase.containsKey(key)){
                weatherDatabase.replace(key, wd);
            }else{
                weatherDatabase.put(key, wd);
            }
        }else{
            //create an empty one if not found
            if(!weatherDatabase.containsKey(key)) {
                weatherDatabase.put(key, new WeatherData());
            }
        }
    }
    private String formCityKey(String[] s){
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length - 1; i++) {
           sb.append(s[i].toLowerCase()).append(",");
        }
        sb.append(s[s.length -1].toLowerCase());
        return sb.toString();
    }
    private String getSearchParams(String[] city){
        if(city[2].isEmpty()||city[3].isEmpty()){
            //return getLookupName(city[0])+" "+getLookupName(city[1]);
            return getLookupName(city[0], city[1]);
        }else{
            return city[2]+" "+city[3];
        }
    }
    public WeatherData getWeatherData(String country, String province, String city, String county){
        WeatherData wd = null;
        String key = formCityKey(new String[]{province,city});
        wd = weatherDatabase.get(key);
        return wd;
    }
    public WeatherData getWeatherData(String citypath){
        WeatherData wd = null;
        if(citypath != null && !citypath.isEmpty()){
            String[] path = citypath.split("[ \t]");
            //province city
            if(path.length > 1) {
                String province = path[0].trim().toLowerCase();
                String city = path[1].trim().toLowerCase();
                for(String key : supportedCities.keySet()){
                    String[] k = supportedCities.get(key);
                    if (k[0].replace(" ","").toLowerCase().contains(province)) {
                        if(k[1].replace("[^a-zA-Z]","").toLowerCase().contains(city)) {
                            wd = getWeatherData("", k[0], k[1], "");
                            break;
                        }else if(k[3].equalsIgnoreCase(city)) {
                            wd = getWeatherData("", k[0], k[1], "");
                            break;
                        }
                    }
                }
            }else{
                String city = path[0].trim().toLowerCase();
                for(String key : supportedCities.keySet()){
                    String[] k = supportedCities.get(key);
                    if(k[1].replace("[^a-zA-Z]","").toLowerCase().contains(city)) {
                        wd = getWeatherData("", k[0], k[1], "");
                        break;
                    }else if(k[3].equalsIgnoreCase(city)) {
                        wd = getWeatherData("", k[0], k[1], "");
                        break;
                    }
                }
            }
        }
        if (wd == null) {
            wd = new WeatherData();
            wd.data.put("error", "city not found");
        }
        return wd;
    }
    private void saveWeatherDB() {
    }
    public HashMap<String,WeatherData> readWeatherData(String filename){
        HashMap<String,WeatherData> wds = new HashMap<>();
        if(filename != null && !filename.isEmpty()) {
            Reader in = null;
            try {
                CSVFormat csvFileFormat = CSVFormat.EXCEL.withHeader("mappath","city",
                        "weather", "temp", "last_update").withDelimiter('\t');
                in = new InputStreamReader(new FileInputStream(filename),"UTF-8");
                Iterable<CSVRecord> records = csvFileFormat.parse(in);
                for (CSVRecord record : records) {
                    int recsize = record.size();
                    if(recsize != 5){
                        logger.warn("Invalid line:"+record.toString());
                        continue;
                    }
                    String key = record.get("mappath").toLowerCase();
                    String city = record.get("city");
                    String weather = record.get("weather");
                    String temp = record.get("temp");
                    String update = record.get("last_update");
                    if (key != null && !key.isEmpty() && !key.startsWith("#")){
                        if(city != null && !city.isEmpty()
                                &&weather != null && !weather.isEmpty()
                                &&temp != null && !temp.isEmpty()
                                &&update!= null && !update.isEmpty()
                                ) {
                            WeatherData wd = new WeatherData();
                            wd.data.put(wd.NAME, city);
                            wd.data.put(wd.TEMP, temp);
                            wd.data.put(wd.LAST_UPDATE, update);
                            wd.data.put(wd.WEATHER, weather);
                            wds.put(key,wd);
                            logger.debug(String.format("%s<--[%s,%s,%s,%s]",key, city, weather,temp,
                                    update));
                        }else{
                            logger.error(key+": not a proper record");
                        }
                    }else{
                        logger.warn("Empty value");
                    }
                }
                logger.debug("size:"+supportedCities.size());
            } catch (Exception e) {
                logger.error("Supported cities not loaded from " + filename);
            }finally {
                try{
                    if(in != null){
                        in.close();
                    }
                }catch (Exception e){
                    logger.error("Error closing weather dbfile");
                }
            }
        }else{
            logger.error(filename+": invalid file");
        }
        return wds;
    }
    public void writeWeatherData(String file, HashMap<String,WeatherData> wds){
        CSVPrinter csvFilePrinter = null;
        CSVFormat csvFileFormat = CSVFormat.EXCEL.withHeader("mappath","city",
                "weather", "temp", "last_update");
        //use \t as delimiter
        CSVFormat csvFormat = csvFileFormat.withDelimiter('\t');
        try {
            File f = new File(file);
            logger.debug("deleting old:"+file);
            f.delete();
            Writer out = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file), "UTF-8"));
            csvFilePrinter = new CSVPrinter(out, csvFormat);
            for (String key : wds.keySet()) {
                WeatherData wd = wds.get(key);
                String name = "N/A";
                String temp = "N/A";
                String weather = "N/A";
                String update = "N/A";
                if(!wd.data.isEmpty()) {
                    name = wd.data.get("name");
                    temp = wd.data.get("temperature");
                    weather = wd.data.get("weather");
                    update = wd.data.get("update");
                }else{
                    //let N/A show to the user
                    wd.data.put("name",name);
                    wd.data.put("temperature",temp);
                    wd.data.put("weather", weather);
                    wd.data.put("update",update);
                }
                csvFilePrinter.printRecord(key,name,weather,temp,update);
            };
            out.flush();
            out.close();
            csvFilePrinter.close();
        }catch (Exception e){
            logger.error("Error writing weather data to file");
        }
    }


    private void loadSupportedCities(){
        if(!supportedCities.isEmpty()){
            logger.info("cities loaded already!");
            return;
        }
        String filename = Config.getInstance().getWeatherConfig().get("citylist").toString();
        logger.info("Loading supported cities from:"+filename);
        if(filename != null && !filename.isEmpty()) {
            Reader in = null;
            try {
                in = new InputStreamReader(new FileInputStream(filename),"UTF-8");
                Iterable<CSVRecord> records = CSVFormat.EXCEL
                        .withHeader("province","city","sprovince",
                                "scity").parse(in);
                for (CSVRecord record : records) {
                    int recsize = record.size();
                    if(recsize<2){
                        logger.warn("Invalid line:"+record.toString());
                        continue;
                    }
                    String state = record.get("province");
                    String city = record.get("city");
                    String sstate = "";
                    String scity = "";
                    if(recsize >= 4) {
                        sstate = record.get("sprovince");
                        scity = record.get("scity");
                    }
                    if (state != null && !state.isEmpty()
                            && !state.startsWith("#")
                            && city != null && !city.isEmpty()) {
                        String key = formCityKey(new String[]{state,city});
                        String[] cityinfo = new String[]{state, city, sstate,scity};
                        logger.debug(String.format("%s->[%s,%s,%s,%s]",key, state,
                                city, sstate,scity));
                        supportedCities.put(key.trim(), cityinfo);
                    } else {
                        logger.warn("Empty value");
                    }
                }
                logger.debug("size:"+supportedCities.size());
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Supported cities not loaded from " + filename);
            } finally {
                try {
                    if(in != null) in.close();
                }catch (Exception e){
                    logger.error("file close Exception");
                }
            }
        }else{
            logger.error("Supported cities not set:" + filename);
        }
    }
    private String getLookupName(String province, String city){
        String first, second;
        if(province.equalsIgnoreCase("shaanxi")){
            first = "shanxi";
        }else if(province.equalsIgnoreCase("Nei mongol")){
            first = "neimonggu";
        }else if(province.equalsIgnoreCase("Ningxia hui")){
            first = "ningxia";
        }else if(province.equalsIgnoreCase("xinjiang uygur")){
            first = "xinjiang";
        }else if(province.equalsIgnoreCase("yunnan")){
            first ="";
        }else{
            first = province;
        }
        String [] cityparts = city.split("[ \t]");
        second = cityparts[0].replaceAll("[^a-zA-Z]","");
        if(first.isEmpty()){
            return second;
        }else {
            return first + " " + second;
        }
    }
    private String getLookupName(String name){
        String [] parts = name.split("[^a-zA-Z]");
        String first = parts[0];
        String ret;
        if(first.equalsIgnoreCase("shaanxi")){
            ret = "shanxi";
        }
        return first;
    }
    private void loadSyncInterval(){
        String syncInterval = Config.getInstance().getWeatherConfig().get("syncinterval").toString();
        if(syncInterval != null && !syncInterval.isEmpty()) {
            int interval = Integer.parseInt(syncInterval);
            if (interval < 2) {
                interval = 2;
            } else if (interval > 12) {
                interval = 12;
            }
            cityRefreshInterval = interval * HOUR;
        }
        logger.info("Refresh interval set to " + cityRefreshInterval/HOUR + " hours.");
    }
}
