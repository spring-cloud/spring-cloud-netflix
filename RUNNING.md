# Running Spring Platform components

## Config Server

  `spring-platform-config$ java -Dspring.platform.config.server.uri=https://github.com/spencergibb/config-repo -jar spring-platform-config-server/target/spring-platform-config-server-1.0.0.BUILD-SNAPSHOT.jar`
  
## Netflix Eureka

  `spring-boot-eureka$ mvn spring-boot:run`
  
  http://localhost:8080/eureka/jsp/status.jsp
  
  Error running executable war:
  
    2014-07-14 18:14:25,713 ERROR org.apache.juli.logging.DirectJDKLog:185 [localhost-startStop-1] [log] Exception starting filter servletContainer
    com.sun.jersey.core.spi.scanning.ScannerException: IO error when scanning jar jar:file:/Users/sgibb/workspace/spring/spring-boot-eureka/target/eureka-server-1.0.0.BUILD-SNAPSHOT.war!/WEB-INF/lib/jersey-apache-client4-1.11.jar!/com/sun/jersey
    	at com.sun.jersey.core.spi.scanning.uri.JarZipSchemeScanner.scan(JarZipSchemeScanner.java:82)
    	at com.sun.jersey.core.spi.scanning.PackageNamesScanner.scan(PackageNamesScanner.java:223)
    	at com.sun.jersey.core.spi.scanning.PackageNamesScanner.scan(PackageNamesScanner.java:139)
    	at com.sun.jersey.api.core.ScanningResourceConfig.init(ScanningResourceConfig.java:80)
    	at com.sun.jersey.api.core.PackagesResourceConfig.init(PackagesResourceConfig.java:104)
    	at com.sun.jersey.api.core.PackagesResourceConfig.<init>(PackagesResourceConfig.java:78)
    	at com.sun.jersey.api.core.PackagesResourceConfig.<init>(PackagesResourceConfig.java:89)
    	at com.sun.jersey.spi.container.servlet.WebComponent.createResourceConfig(WebComponent.java:700)
    	at com.sun.jersey.spi.container.servlet.WebComponent.createResourceConfig(WebComponent.java:678)
    	at com.sun.jersey.spi.container.servlet.WebComponent.init(WebComponent.java:203)
    	at com.sun.jersey.spi.container.servlet.ServletContainer.init(ServletContainer.java:374)
    	at com.sun.jersey.spi.container.servlet.ServletContainer.init(ServletContainer.java:727)
    	at org.apache.catalina.core.ApplicationFilterConfig.initFilter(ApplicationFilterConfig.java:279)
    	at org.apache.catalina.core.ApplicationFilterConfig.<init>(ApplicationFilterConfig.java:109)
    	at org.apache.catalina.core.StandardContext.filterStart(StandardContext.java:4809)
    	at org.apache.catalina.core.StandardContext.startInternal(StandardContext.java:5485)
    	at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:150)
    	at org.apache.catalina.core.ContainerBase$StartChild.call(ContainerBase.java:1559)
    	at org.apache.catalina.core.ContainerBase$StartChild.call(ContainerBase.java:1549)
    	at java.util.concurrent.FutureTask.run(FutureTask.java:262)
    	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1145)
    	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:615)
    	at java.lang.Thread.run(Thread.java:744)
    Caused by: java.io.FileNotFoundException: /Users/sgibb/workspace/spring/spring-boot-eureka/target/eureka-server-1.0.0.BUILD-SNAPSHOT.war!/WEB-INF/lib/jersey-apache-client4-1.11.jar (No such file or directory)
    	at java.io.FileInputStream.open(Native Method)
    	at java.io.FileInputStream.<init>(FileInputStream.java:146)
    	at java.io.FileInputStream.<init>(FileInputStream.java:101)
    	at sun.net.www.protocol.file.FileURLConnection.connect(FileURLConnection.java:90)
    	at sun.net.www.protocol.file.FileURLConnection.getInputStream(FileURLConnection.java:188)
    	at java.net.URL.openStream(URL.java:1037)
    	at com.sun.jersey.core.spi.scanning.uri.JarZipSchemeScanner.closing(JarZipSchemeScanner.java:123)
    	at com.sun.jersey.core.spi.scanning.uri.JarZipSchemeScanner.scan(JarZipSchemeScanner.java:75)
    	... 22 more
  
  
## Netflix Zuul

  `spring-platform-netflix-zuul$ java -Dzuul.filterRoot=src/main/resources/filters -jar target/spring-platform-netflix-zuul-1.0.0.BUILD-SNAPSHOT.jar`
  
  http://localhost:6080/ (routes to Sample Frontend)
  
## Netflix Hystrix Dashboard

  `spring-platform-netflix-hystrix$ java -jar target/spring-platform-netflix-hystrix-1.0.0.BUILD-SNAPSHOT.war`
  
## Netflix Turbine
 
  `spring-platform-netflix-turbine$ java -jar target/spring-platform-netflix-turbine-1.0.0.BUILD-SNAPSHOT.war --turbine.aggregator.clusterConfig=sampleApps --turbine.ConfigPropertyBasedDiscovery.sampleApps.instances=localhost:9080 --turbine.instanceUrlSuffix=/hystrix.stream`
  
## Sandbox Sample Backend

  `spring-platform-sandbox-sample-backend$ java -jar target/spring-platform-sandbox-sample-backend-1.0.0.BUILD-SNAPSHOT.jar`0
  
## Sandbox Sample Frontend

  `spring-platform-sandbox-sample-frontend$ java -jar target/spring-platform-sandbox-sample-frontend-1.0.0.BUILD-SNAPSHOT.jar`
  
  http://localhost:9080/
