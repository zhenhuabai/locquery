set map=China
set CLASSPATH=c:\gis\locquery
vertx run -cp target\locquery-0.0.1-fat.jar com.huleibo.CountryMapServer --cluster

set map=Global
set CLASSPATH=c:\gis\locquery
vertx run -cp target\locquery-0.0.1-fat.jar com.huleibo.CountryMapServer --cluster

vertx run -cp target\locquery-0.0.1-fat.jar com.huleibo.LocQueryVerticle --cluster
