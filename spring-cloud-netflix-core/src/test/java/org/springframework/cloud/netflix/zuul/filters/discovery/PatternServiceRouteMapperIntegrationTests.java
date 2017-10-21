package org.springframework.cloud.netflix.zuul.filters.discovery;

import java.util.ArrayList;
import java.util.List;

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.netflix.zuul.context.RequestContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.RoutesMvcEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.netflix.zuul.filters.discovery.PatternServiceRouteMapperIntegrationTests.SERVICE_ID;

/**
 * @author Stéphane Leroy
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SampleCustomZuulProxyApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT, value = {
		"spring.application.name=regex-test-application", "spring.jmx.enabled=false",
		"eureka.client.enabled=false" })
@DirtiesContext
public class PatternServiceRouteMapperIntegrationTests {

	protected static final String SERVICE_ID = "domain-service-v1";

	@Value("${local.server.port}")
	private int port;

	@Autowired
	private DiscoveryClientRouteLocator routes;

	@Autowired
	private RoutesMvcEndpoint endpoint;

	@Before
	public void setTestRequestcontext() {
		RequestContext context = new RequestContext();
		RequestContext.testSetCurrentContext(context);
	}

	@After
	public void clear() {
		RequestContext.getCurrentContext().clear();
	}

	@Test
	public void getRegexMappedService() {
		this.endpoint.reset();
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/v1/domain/service/get/1",
				HttpMethod.GET, new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Get 1", result.getBody());
	}

	@Test
	public void getStaticRoute() {
		this.routes.addRoute("/self/**", "http://localhost:" + this.port);
		this.endpoint.reset();
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/self/get/1", HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Get 1", result.getBody());
	}

}

@Configuration
@EnableAutoConfiguration
@RestController
@EnableZuulProxy
@RibbonClient(value = SERVICE_ID, configuration = SimpleRibbonClientConfiguration.class)
class SampleCustomZuulProxyApplication {

	@Bean
	public DiscoveryClient discoveryClient() {
		DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
		List<String> services = new ArrayList<>();
		services.add(SERVICE_ID);
		when(discoveryClient.getServices()).thenReturn(services);
		return discoveryClient;
	}

	@RequestMapping(value = "/get/{id}", method = RequestMethod.GET)
	public String get(@PathVariable String id) {
		return "Get " + id;
	}

	@Bean
	public PatternServiceRouteMapper serviceRouteMapper() {
		return new PatternServiceRouteMapper(
				"(?<domain>^.+)-(?<name>.+)-(?<version>v.+$)",
				"${version}/${domain}/${name}");
	}

	public static void main(String[] args) {
		SpringApplication.run(SampleCustomZuulProxyApplication.class, args);
	}
}

@Configuration
class SimpleRibbonClientConfiguration {

	@Value("${local.server.port}")
	private int port = 0;

	@Bean
	public ServerList<Server> ribbonServerList() {
		return new StaticServerList<>(new Server("localhost", this.port));
	}
}
