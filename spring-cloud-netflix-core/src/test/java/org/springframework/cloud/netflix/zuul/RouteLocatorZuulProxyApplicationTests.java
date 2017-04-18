package org.springframework.cloud.netflix.zuul;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.cloud.netflix.zuul.ZuulProxyTestBase.AbstractZuulProxyApplication;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.cloud.netflix.zuul.filters.SimpleRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.zuul.context.RequestContext;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = RouteLocatorZuulProxyApplicationConfiguration.class)
@WebAppConfiguration
@IntegrationTest({ "server.port: 0", "zuul.routes.other: /test/**=http://localhost:7777/local",
		"zuul.routes.another: /another/twolevel/**", "zuul.routes.simple: /simple/**",
		"zuul.routes.badhost: /badhost/**", "zuul.ignoredHeaders: X-Header", "zuul.routes.rnd: /rnd/**",
		"rnd.ribbon.listOfServers: ${random.value}", "zuul.removeSemicolonContent: false" })
@DirtiesContext
public class RouteLocatorZuulProxyApplicationTests {

	// Heavily copied from SampleZuulProxyApplicationTest and ZuulProxyTestBase
	
	
	@Autowired
	RouteLocator routeLocator;
	
	@Autowired
	RoutesEndpoint endpoint;
	
	@Value("${local.server.port}")
	protected int port;

	@Before
	public void setTestRequestcontext() {
		RequestContext.testSetCurrentContext(null);
		RequestContext.getCurrentContext().unset();
	}

	@Test
	public void testType() {
		assertNotNull(routeLocator);
		assertEquals(CustomRouteLocator.class, routeLocator.getClass());
	}
	
	@Test
	public void testNonExistantRoute() {
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/not-found", HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());	
	}
	
	@Test
	public void testCustomRoute() {
		CustomRouteLocator crl = (CustomRouteLocator) routeLocator;
		crl.addRoute("/self/**", "http://localhost:" + this.port);
		endpoint.reset();
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port
						+ "/self/qstring?override=true&different=key",
				HttpMethod.GET, new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Received {key=[overridden]}", result.getBody());
	}
	

}

@Configuration
@EnableAutoConfiguration
@RestController
@EnableZuulProxy
class RouteLocatorZuulProxyApplicationConfiguration extends AbstractZuulProxyApplication {

	@Autowired
	protected ZuulProperties zuulProperties;

	@Autowired
	protected ServerProperties server;

	@Bean
	RouteLocator routeLocator() {
		return new CustomRouteLocator(this.server.getServletPrefix(), zuulProperties);
	}

}

class CustomRouteLocator extends SimpleRouteLocator {

	private ZuulProperties properties;

	public CustomRouteLocator(String servletPath, ZuulProperties properties) {
		super(servletPath, properties);
		this.properties = properties;
	}

	public void addRoute(String path, String location) {
		this.properties.getRoutes().put(path, new ZuulRoute(path, location));
		doRefresh();
	}

}