package org.springframework.cloud.netflix.zuul.sample;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.cloud.netflix.zuul.RouteLocator;
import org.springframework.cloud.netflix.zuul.ZuulHandlerMapping;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = ZuulProxyApplication.class)
@WebAppConfiguration
@IntegrationTest("server.port=0")
public class ZuulProxyApplicationTests {

	@Value("${local.server.port}")
	private int port;

	@Autowired
	private RouteLocator routes;

	@Autowired
	private ZuulHandlerMapping mapping;

	@Test
	public void deleteOnSelf() {
		routes.getRoutes().put("/self/**", "http://localhost:" + port + "/local");
		mapping.reset();
		ResponseEntity<String> result = new TestRestTemplate().exchange("http://localhost:" + port + "/self/1",
				HttpMethod.DELETE, new HttpEntity<Void>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
	}

}
