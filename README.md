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
