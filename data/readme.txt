1.ȫ�°�װ
    a.����
        Linux x64
        java 1.8�������л��������ص�ַ��
            http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
        �ڵ�¼��java -versionӦ����ʾ��ȷjava�汾��Ϣ
    b.����
        ���ļ�locquery_r_0_0_6.tar.gz�ƶ�����װĿ¼
        mv locquery_r_0_0_6.tar.gz install
        cd install
        tar xfz locquery_release_0_0_6.tar.gz
        ʹ���ļ�˵��
            startsvr ���������ļ�
            config.json �����ļ�
            chinese.csv �ο�ƴ��-����������ӳ���ļ����ο�ǰ�������в�ȫ
            logs ��־�ļ�Ŀ¼
            lib ���п��ļ�
                supportedcities.csv ֧��������ѯ�ĳ���
            data ����Ŀ¼
                weatherdb.csv ���������ļ�

    c.��ͼ��װ
        ��http://www.diva-gis.org/gdata���أ�Countryѡ��"China", Subjectѡ��"Administrative areas".��ѹ������12���ļ�����������Ŀ¼����config.json��ָ��CHN_adm0.shp ��CHN_adm3.shp�ļ���·��
     CHN_adm0.dbf   CHN_adm3.dbf
     CHN_adm0.prj   CHN_adm3.prj
     CHN_adm0.sbn   CHN_adm3.sbn
     CHN_adm0.sbx   CHN_adm3.sbx
     CHN_adm0.shp   CHN_adm3.shp
     CHN_adm0.shx   CHN_adm3.shx

2.������װ
    a.�رճ��򣺿���ͨ�� ps -eo pid,cmd|grep huleibo|grep -v grep|awk '{print $1}'|xargs kill -9
    a.���ļ�չ����Ŀ¼������~/update
    b.ɾ��ԭĿ¼�� lib/locquery.*jar
    c.��~/update/lib/locquery.*jar�ļ�������ԭĿ¼lib/
    d.��ԭĿ¼�½�dataĿ¼������~/update/data/watherdb.csv����Ŀ¼�¡����ļ������ϴθ��¹������ݣ����Ը��������ݡ�Ҳ����ʡ�Դβ���������������δ����ǰû���������ݡ�
    e.��~/update/lib/supportedcities.csv����ԭ����libĿ¼
3.��������
    ��Ҫ��config.json�������������֧��������ѯ���ο��������Ӹ���
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
          "xinzhi":"z1rpwxehv8jexdwg" ��������Ϊ��֪������http://www.thinkpage.cn/api)����Կ���п��ܹ��ڣ������������ע����˺�
        },
        "syncinterval": 4 ��������������ͬ��������ͬ���ļ������λСʱ����Чֵ2-12
      },
    ����λ�÷�������Ҫ��config.json�м������£��ο�����
        "locationmanager" : {
            "mongodburl": "mongodb://localhost:27017", ���ݿ�URL
            "database": "locquery", ���ݿ�����
            "locationanalyzer" : {
                "pastdays":90, ����90�����ڵ�λ����Ϣ��¼ >0
                "thresholdpercentage": 0.8, λ����Ϣ�����һ�����г��ָ��ʴ���80%��Ϊ��������Ǳ��أ�0-1
                "scaninterval":10 ��������೤ʱ��ɨ��һ���ϴ���λ����Ϣ����λ���ӣ�>1
            }
        },

4.����
    ����./startsvr
    a.���к�����http GET�ṩ���в�ѯ���������ʽ��
            http://[����/IP:�˿�]/api/city?lon=����&lat=γ��[&lang=��λ��ĸ��������Ŀǰֻ֧��zh,��������Ϊen]
        ������������:
            {"country":"�й�","province":"����ʡ","city":"�Ͼ���","county":"����","lang":"zh"}
        ���û����Ӧ���ķ��أ�
            {"country":"China","province":"Jiangsu","city":"Nanjing","county":"Luhe","lang":"en"}
        �����ͼ�в����������λ�ã����ؿ�����{}

    b.���к�����http GET�ṩ����������ѯ���������ʽ��
        http://[����/IP:�˿�]/api/weather?location=[����,γ��]|[��������ƴ��]
        ����:api/weather?location=beijing������:{"name":"����","temperature":"-8","weather":"��","update":"2017-01-23T01:25:00+08:00"}
        ����:api/weather?location=118,34������:{"name":"����","temperature":"-5","weather":"��","update":"2017-01-23T02:15:00+08:00"}
        ����:api/weather?location=jiangsu changzhou,����:{"name":"����","temperature":"-2","weather":"����","update":"2017-01-23T02:10:00+08:00"}

    c.�û�����λ���ϴ�
        http://[����/IP:�˿�]/api/userlocation
            ����POST ���ݸ�ʽ json
            {
             "uid":100006, uid, �û�id
             "lon":116.395645, ����
             "lat":39.929986, γ��
             "timestamp":1480377600000, epoch/unixʱ�䣬���룬GMT
            }
            �ɹ����� code 200 json
            {
                "result": "OK"
            }
            ʧ�ܷ��� code ��200 statusMessage��ԭ��

    d.�����û������ء��û�֮ǰ����ֵ�ᱻ�滻
        http://[����/IP:�˿�]/api/userlocal
            ����PUT ���ݸ�ʽ json
            {
                "uid":100011, �û�id
                "analyzerAllowed":false,�Ƿ���������������ñ��ؽ��
                "cityinfo": Ҫ���õı�����Ϣ
                {
                    "en": Ӣ�ģ���һ�����ñ���������Ļ���Ӣ������һ������Ȼ����ʧ��
                    {
                        "province":"Jiangxi","city":"Nanchang"
                    },
                    "zh": ����
                    {
                        "province":"����ʡ",
                        "city":"�ϲ���"
                    }
                }
            }
            �ɹ����� code 200 json
            {
                "result": "OK"
            }
            ʧ�ܷ��� code ��200 statusMessage��ԭ��

    e.��ȡ�û������ء�
        http://[����/IP:�˿�]/api/userlocal?uid=[�û�id 100006]&lang=[���� en����zh]
            ����GET
             �ɹ�����code 200 ���ݸ�ʽ json
             {
                 "result":"OK",�����ɹ�
                  "data":[ ����
                  {
                      "uid":100011,�û�id
                      "locals": [�����б�
                       {
                           "lang":"zh",������Ϣ����
                           "cityinfo":������Ϣ
                           {
                               "province":"����ʡ",
                               "city":"�ϲ���"
                           },
                           "probability":0.5 �������ϴ���ʷ�и��ʣ�������������Ϊ1.0
                       },
                       {
                           "lang":"zh",������Ϣ����
                           "cityinfo":������Ϣ
                           {
                               "province":"����ʡ",
                               "city":"�Ͼ���"
                           },
                           "probability":0.5
                       }
                      ]
                  }
                 ]
             }
            ʧ�ܷ��� ��200 statusMessage��ԭ��

    e. �Ƿ���ء�
        http://[����/IP:�˿�]/api/isnonlocal?uid=[�û�id 100001]&location=[���� 32.1,γ�� 108.3]&probability=[���� 0.9]
            ����GET
             �ɹ�����code 200 ���ݸ�ʽ json
             {
                 "result":"OK",�����ɹ�
                  "data": true ��ָ������������أ�false ��
             }
            ʧ�ܷ��� ��200 statusMessage��ԭ��

5.˵��
    a.��ͼ
        ����Ŀǰʵ�����ܹ��㣬1�˷������Ĵ�����������Ϊ1�봦��50�����ҵ�����1�봦��1000��������������Ҫ3̨8�˵ķ�����֧�֡�
        ������8��16G������Ϊ�������á�һ̨��������һ���ڴ������400������
        Ӳ�����ܶԴ����ٶ�Ӱ��δ������
        *��ͬ���ҵ�ͼ�Ĳ�ѯ�ٶȲ�һ����
    b.����
        Ŀǰ��ͼ��������344���ؼ��У����������ݡ���֪������ѵĳ��в�����400����ѡ��֪����Ϊ���Ǵ�ķ����̣���ѳ��ж࣬�����٣���������һСʱ����400�β�ѯ��Ŀǰ����12Сʱˢ��һ�����ݡ�
        i. ���ڵ�ͼ�е�ƴ���������Թ��⣬�����ܺͺ���ƴ����ȫ��Ӧ����������ɹ�д��,"Nei Mongol" ����������վ��Ҫ"neimenggo"��������Ҫת����
        ��Щ����������������������Ƚϼ��С�
        �����������supportedcities.csv�ж���ӳ��.�����½������յ������Զ���Ϊ��
        Xinjiang Uygur,Aksu,Xinjiang,akesu
        ������ѯ��ʱ�����������ƴ����Ҳ���ܲ鵽�����ˡ�
        ii. ����һ��������Ϊ����������������У���������Ҫ���������׸�������������������������������˫���ɴ��������ݶ������£�
        Yunnan,Xishuangbanna Dai,Yunnan,Jinghong
        iii. ����һЩ�ǲ�����ѽ�ڿ��ŵ�:���糲��
        Ŀǰ����44�����Ƶĵط�û�ж��壬���г�����unfound.csv����Щ�������˸���i.  ii. supportedcities.csv�н������ѽ�ڲ����ŵľͼ�����Ҳû�취�ˡ�
    c.���ڷ�������
        Ŀǰ����������ÿ10����ɨ��һ��λ���ϴ���¼�������ɷ�����������ڵ�һ�û���˵��λ�ò���䶯̫Ƶ�������˾���10����һ�ε�Ƶ���㹻��ȷ�����Ե�ʱ�������Ϊ��С��1���ӡ����Ҫ���ϴ��Ľ�������ڲ�ѯ�У���Ҫ�ȴ��趨��ʱ���������ֳ�������������Ϊ1����ô�ϴ���ķ��������1���Ӻ���׼ȷ�ġ�
