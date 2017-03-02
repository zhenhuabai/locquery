1.全新安装
    a.环境
        Linux x64
        java 1.8以上运行环境，下载地址：
            http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
        在登录后java -version应能显示正确java版本信息
    b.方法
        将文件locquery_r_0_0_6.tar.gz移动到安装目录
        mv locquery_r_0_0_6.tar.gz install
        cd install
        tar xfz locquery_release_0_0_6.tar.gz
        使用文件说明
            startsvr 服务启动文件
            config.json 配置文件
            chinese.csv 参考拼音-中文县市名映射文件，参考前几行自行补全
            logs 日志文件目录
            lib 运行库文件
                supportedcities.csv 支持天气查询的城市
            data 数据目录
                weatherdb.csv 天气数据文件

    c.地图安装
        从http://www.diva-gis.org/gdata下载，Country选择"China", Subject选择"Administrative areas".解压后将以下12个文件拷贝到合适目录并在config.json中指名CHN_adm0.shp 和CHN_adm3.shp文件的路径
     CHN_adm0.dbf   CHN_adm3.dbf
     CHN_adm0.prj   CHN_adm3.prj
     CHN_adm0.sbn   CHN_adm3.sbn
     CHN_adm0.sbx   CHN_adm3.sbx
     CHN_adm0.shp   CHN_adm3.shp
     CHN_adm0.shx   CHN_adm3.shx

2.升级安装
    a.关闭程序：可以通过 ps -eo pid,cmd|grep huleibo|grep -v grep|awk '{print $1}'|xargs kill -9
    a.将文件展开到目录，比如~/update
    b.删除原目录下 lib/locquery.*jar
    c.把~/update/lib/locquery.*jar文件拷贝到原目录lib/
    d.在原目录新建data目录，拷贝~/update/data/watherdb.csv到该目录下。该文件包含上次更新过得数据，可以给出旧数据。也可以省略次步，但请求在数据未更新前没有天气数据。
    e.把~/update/lib/supportedcities.csv考入原来的lib目录
3.更改配置
    需要在config.json里面加入以下项支持天气查询，参考包中例子更改
      "weather" : {
        "citylist": {
          "windows": ".\\lib\\supportedcities.csv",
          "linux": "/opt/locquery/mapcities.2nd.csv",
          "mac": "/Users/dbai/gis/locquery/supportedcities.csv"
        },
        "dbfile":{
          "windows": ".\\data\\weatherdb.csv",
          "linux": "/opt/locquery/weatherdb.csv",
          "mac": "/Users/dbai/gis/locquery/weatherdb.csv"
        },
        "sourcekeys":{
          "xinzhi":"z1rpwxehv8jexdwg" ！！！此为心知天气（http://www.thinkpage.cn/api)的密钥，有可能过期，如果过期重新注册个账号
        },
        "syncinterval": 4 ！！！天气数据同服务器端同步的间隔，单位小时。有效值2-12
      },
    配置位置服务功能需要在config.json中加入以下，参考例子
        "locationmanager" : {
            "mongodburl": "mongodb://localhost:27017", 数据库URL
            "database": "locquery", 数据库名字
            "locationanalyzer" : {
                "pastdays":90, 分析90天以内的位置信息记录 >0
                "thresholdpercentage": 0.8, 位置信息中如果一个城市出现概率大于80%认为这个城市是本地，0-1
                "scaninterval":10 分析程序多长时间扫描一次上传的位置信息，单位分钟，>1
            }
        },

4.运行
    启动./startsvr
    a.运行后会根据http GET提供城市查询服务，请求格式是
            http://[域名/IP:端口]/api/city?lon=经度&lat=纬度[&lang=两位字母语言名，目前只支持zh,其它都视为en]
        返回数据如下:
            {"country":"中国","province":"江苏省","city":"南京市","county":"六合","lang":"zh"}
        如果没有相应中文返回：
            {"country":"China","province":"Jiangsu","city":"Nanjing","county":"Luhe","lang":"en"}
        如果地图中不包含请求的位置，返回空数据{}

    b.运行后会根据http GET提供城市天气查询服务，请求格式是
        http://[域名/IP:端口]/api/weather?location=[经度,纬度]|[城市名称拼音]
        比如:api/weather?location=beijing，返回:{"name":"北京","temperature":"-8","weather":"晴","update":"2017-01-23T01:25:00+08:00"}
        比如:api/weather?location=118,34，返回:{"name":"徐州","temperature":"-5","weather":"晴","update":"2017-01-23T02:15:00+08:00"}
        比如:api/weather?location=jiangsu changzhou,返回:{"name":"常州","temperature":"-2","weather":"多云","update":"2017-01-23T02:10:00+08:00"}

    c.用户地理位置上传
        http://[域名/IP:端口]/api/userlocation
            操作POST 数据格式 json
            {
             "uid":100006, uid, 用户id
             "lon":116.395645, 经度
             "lat":39.929986, 纬度
             "timestamp":1480377600000, epoch/unix时间，毫秒，GMT
            }
            成功返回 code 200 json
            {
                "result": "OK"
            }
            失败返回 code 非200 statusMessage有原因

    d.设置用户“本地”用户之前设置值会被替换
        http://[域名/IP:端口]/api/userlocal
            操作PUT 数据格式 json
            {
                "uid":100011, 用户id
                "analyzerAllowed":false,是否允许分析程序设置本地结果
                "cityinfo": 要设置的本地信息
                {
                    "en": 英文！！一次设置必须包含中文或者英文至少一个，不然返回失败
                    {
                        "province":"Jiangxi","city":"Nanchang"
                    },
                    "zh": 中文
                    {
                        "province":"江西省",
                        "city":"南昌市"
                    }
                }
            }
            成功返回 code 200 json
            {
                "result": "OK"
            }
            失败返回 code 非200 statusMessage有原因

    e.获取用户“本地”
        http://[域名/IP:端口]/api/userlocal?uid=[用户id 100006]&lang=[语言 en或者zh]
            操作GET
             成功返回code 200 数据格式 json
             {
                 "result":"OK",操作成功
                  "data":[ 数据
                  {
                      "uid":100011,用户id
                      "locals": [城市列表
                       {
                           "lang":"zh",城市信息语言
                           "cityinfo":城市信息
                           {
                               "province":"江西省",
                               "city":"南昌市"
                           },
                           "probability":0.5 城市在上传历史中概率，如果不允许分析为1.0
                       },
                       {
                           "lang":"zh",城市信息语言
                           "cityinfo":城市信息
                           {
                               "province":"江苏省",
                               "city":"南京市"
                           },
                           "probability":0.5
                       }
                      ]
                  }
                 ]
             }
            失败返回 非200 statusMessage有原因

    e. 是否“外地”
        http://[域名/IP:端口]/api/isnonlocal?uid=[用户id 100001]&location=[经度 32.1,纬度 108.3]&probability=[概率 0.9]
            操作GET
             成功返回code 200 数据格式 json
             {
                 "result":"OK",操作成功
                  "data": true 在指定概率下是外地，false 否
             }
            失败返回 非200 statusMessage有原因

5.说明
    a.地图
        根据目前实验性能估算，1核服务器的处理请求能力为1秒处理50个左右的请求。1秒处理1000次请求理论上需要3台8核的服务器支持。
        建议以8核16G服务器为基本配置。一台机器可以一秒内处理完成400个请求。
        硬盘性能对处理速度影响未评估。
        *不同国家地图的查询速度不一样。
    b.天气
        目前地图数据中有344个地级市，或者自治州。心知天气免费的城市不超过400个。选心知是因为算是大的服务商，免费城市多，限制少，比如允许一小时调用400次查询。目前程序12小时刷新一次数据。
        i. 由于地图中的拼音名称来自国外，并不能和汉语拼音完全对应。比如把内蒙古写成,"Nei Mongol" ，而天气网站需要"neimenggo"。所以需要转换。
        这些问题尤其在少数民族地区比较集中。
        解决方法是在supportedcities.csv中定义映射.比如新疆阿克苏地区可以定义为：
        Xinjiang Uygur,Aksu,Xinjiang,akesu
        这样查询的时候会用正常的拼音，也就能查到天气了。
        ii. 还有一种问是因为自治区包含多个城市，这样就需要把自治区首府的天气当做自治区的天气，比如西双版纳傣族自治州定义如下：
        Yunnan,Xishuangbanna Dai,Yunnan,Jinghong
        iii. 还有一些是不给免费借口开放的:比如巢湖
        目前还有44个类似的地方没有定义，我列出来到unfound.csv。这些可以找人根据i.  ii. supportedcities.csv中解决。免费借口不开放的就几个，也没办法了。
    c.关于分析程序
        目前分析程序是每10分钟扫描一次位置上传记录表，并生成分析结果。对于单一用户来说，位置不会变动太频繁，个人觉得10分钟一次的频率足够精确。测试的时候可以设为较小的1分钟。如果要让上传的结果体现在查询中，需要等待设定的时间后才能体现出来。比如设置为1，那么上传后的分析结果在1分钟后是准确的。
