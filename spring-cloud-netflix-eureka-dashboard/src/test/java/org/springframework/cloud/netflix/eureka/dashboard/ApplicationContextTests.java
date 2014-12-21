package org.springframework.cloud.netflix.eureka.dashboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.cloud.netflix.eureka.dashboard.ApplicationContextTests.Application;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@IntegrationTest({ "server.port=0", "spring.application.name=eureka", "server.contextPath=/context" })
public class ApplicationContextTests {

	@Value("${local.server.port}")
	private int port = 0;

	@Configuration
	@EnableAutoConfiguration
	@EnableEurekaServer
	@EnableEurekaDashboard
	protected static class Application {
		public static void main(String[] args) {
			new SpringApplicationBuilder(Application.class).properties("spring.application.name=eureka", "server.contextPath=/context").run(args);
		}
	}

	@Test
	public void catalogLoads() {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = new TestRestTemplate().getForEntity("http://localhost:" + port + "/context/eureka/apps", Map.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
	}

	@Test
	public void dashboardLoads() {
		ResponseEntity<String> entity = new TestRestTemplate().getForEntity("http://localhost:" + port + "/context/", String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		String body = entity.getBody();
		// System.err.println(body);
		assertTrue(body.contains("webjars/"));
		assertTrue(body.contains("css"));
		// The "DS Replicas"
		assertTrue(body.contains("<a href=\"http://localhost:8761/eureka/\">localhost</a>"));
	}

	@Test
	public void cssAvailable() {
		ResponseEntity<String> entity = new TestRestTemplate().getForEntity("http://localhost:" + port + "/context/css/wro.css", String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
	}

	@Test
	public void adminLoads() {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = new TestRestTemplate().getForEntity("http://localhost:" + port + "/context/env", Map.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
	}

}
