package org.springframework.cloud.netflix.eureka.server;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.cloud.netflix.eureka.server.ApplicationContextTests.Application;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@IntegrationTest({ "server.port=0", "spring.application.name=eureka",
		"eureka.dashboard.path=/dashboard" })
public class ApplicationDashboardPathTests {

	@Value("${local.server.port}")
	private int port = 0;

	@Test
	public void catalogLoads() {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = new TestRestTemplate().getForEntity(
				"http://localhost:" + this.port + "/eureka/apps", Map.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
	}

	@Test
	public void dashboardLoads() {
		ResponseEntity<String> entity = new TestRestTemplate().getForEntity(
				"http://localhost:" + this.port + "/dashboard", String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		String body = entity.getBody();
		// System.err.println(body);
		assertTrue(body.contains("eureka/js"));
		assertTrue(body.contains("eureka/css"));
		// The "DS Replicas"
		assertTrue(body
				.contains("<a href=\"http://localhost:8761/eureka/\">localhost</a>"));
		// The Home
		assertTrue(body.contains("<a href=\"/dashboard\">Home</a>"));
		// The Lastn
		assertTrue(body.contains("<a href=\"/dashboard/lastn\">Last"));
	}

	@Test
	public void cssAvailable() {
		ResponseEntity<String> entity = new TestRestTemplate().getForEntity(
				"http://localhost:" + this.port + "/eureka/css/wro.css", String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
	}

	@Test
	public void jsAvailable() {
		ResponseEntity<String> entity = new TestRestTemplate().getForEntity(
				"http://localhost:" + this.port + "/eureka/js/wro.js", String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
	}

}
