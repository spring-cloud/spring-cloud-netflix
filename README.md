Ths project provides Netflix OSS integrations for Spring Boot apps through autoconfiguration
and binding to the Spring Environment and other Spring programming model idioms.

# Eureka Clients

Example eureka client:

```
@Configuration
@ComponentScan
@EnableAutoConfiguration
@RestController
public class Application {

	@RequestMapping("/")
	public String home() {
		return "Hello world";
	}
	
	public static void main(String[] args) {
		new SpringApplicationBuilder(Application.class).web(true).run(args);
	}

}

```

(i.e. utterly normal Spring Boot app). Configuration is required to locate the Eureka server. Example:

```
eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8080/v2/
      default.defaultZone: http://localhost:8080/v2/
```

The default application name, virtual host and non-secure port are taken from the `Environment` is 
`${spring.application.name}`, `${spring.application.name}.mydomain.net` and `${server.port}` respectively.

# TODO List

- [x] Example front end app (currently in sandbox)
- [x] Example back end service (currently in sandbox)
- [x] Use platform config
- [x] Hystrix integration (hystrix-javanica)
- [x] Feign use spring message converters
- [x] Ribbon (static server list)
- [x] Eureka boot app (Service registration)
- [x] Eureka (apache -> tomcat) see https://github.com/cfregly/fluxcapacitor/wiki/NetflixOSS-FAQ#eureka-service-discovery-load-balancer and https://groups.google.com/forum/?fromgroups#!topic/eureka_netflix/g3p2r7gHnN0
- [x] Archaius bridge to spring environment
- [x] Ribbon (Client side load balancing) (Eureka integration)
  - [x] Remove need for *-eureka.properties
  - [ ] Use spring boot values as defaults where appropriate
  - [x] Synchronous removal of service from eureka on shutdown
- [x] Refresh log levels dynamically
- [x] Router (Zuul) integrated using hystrix/ribbon/eureka
- [ ] Better observable example
- [ ] Distributed refresh environment via platform bus
- [x] Metrics aggregation (turbine)
  - [x] Use Eureka for instance discovery rather than static list see https://github.com/Netflix/Turbine/blob/master/turbine-contrib/src/main/java/com/netflix/turbine/discovery/EurekaInstanceDiscovery.java
  - [ ] Configure InstanceDiscovery.impl using auto config/config props
