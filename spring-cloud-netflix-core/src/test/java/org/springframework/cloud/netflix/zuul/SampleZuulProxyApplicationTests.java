package org.springframework.cloud.netflix.zuul;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SampleZuulProxyApplication.class)
@WebAppConfiguration
@IntegrationTest({ "server.port: 0",
		"zuul.routes.other: /test/**=http://localhost:7777/local",
		"zuul.routes.simple: /simple/**" })
public class SampleZuulProxyApplicationTests {

	@Value("${local.server.port}")
	private int port;

	@Autowired
	private ZuulRouteLocator routes;

	@Autowired
	private ZuulHandlerMapping mapping;

	@Test
	public void bindRouteUsingPropertyEditor() {
		assertEquals("http://localhost:7777/local", routes.getRoutes().get("/test/**"));
	}

	@Test
	public void bindRouteUsingOnlyPath() {
		assertEquals("simple", routes.getRoutes().get("/simple/**"));
	}

	@Test
	public void deleteOnSelfViaSimpleHostRoutingFilter() {
		routes.getRoutes().put("/self/**", "http://localhost:" + port + "/local");
		mapping.registerHandlers(routes.getRoutes().keySet());
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + port + "/self/1", HttpMethod.DELETE,
				new HttpEntity<Void>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Deleted!", result.getBody());
	}

}
