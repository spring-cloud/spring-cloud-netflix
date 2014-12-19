package org.springframework.cloud.netflix.hystrix;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = HystrixOnlyApplication.class)
@WebAppConfiguration
@IntegrationTest({ "server.port: 0" })
@DirtiesContext
public class HystrixOnlyTests {

	@Value("${local.server.port}")
	private int port;

	@Test
	public void testNormalExecution() {
		String s = new TestRestTemplate().getForObject("http://localhost:" + port + "/", String.class);
		assertEquals("incorrect response", "Hello world", s);
	}

	@Test
	public void testFailureFallback() {
		String s = new TestRestTemplate().getForObject("http://localhost:" + port + "/fail", String.class);
		assertEquals("incorrect fallback", "Fallback Hello world", s);
	}

	@Test
	public void testHystrixHealth() {
		Map map = getHealth();
		assertTrue("Missing hystrix health key", map.containsKey("hystrix"));
		Map hystrix = (Map) map.get("hystrix");
		assertEquals("Wrong hystrix status", "UP", hystrix.get("status"));
	}

	@Test
	public void testNoDiscoveryHealth() {
		Map map = getHealth();
		//There is explicitly no discovery, so there should be no discovery health key
		assertFalse("Incorrect existing discovery health key", map.containsKey("discovery"));
	}

	private Map getHealth() {
		return new TestRestTemplate().getForObject("http://localhost:" + port + "/admin/health", Map.class);
	}
}

class Service {
	@HystrixCommand
	public String hello() {
		return "Hello world";
	}

	@HystrixCommand(fallbackMethod = "fallback")
	public String fail() {
		throw new RuntimeException("Always fail");
	}

	public String fallback() {
		return "Fallback Hello world";
	}
}

//Don't use @SpringBootApplication because we don't want to component scan
@Configuration
@EnableAutoConfiguration
@EnableCircuitBreaker
@RestController
class HystrixOnlyApplication {

	@Bean
	public Service service() {
		return new Service();
	}

	@Autowired
	Service service;

	@RequestMapping("/")
	public String home() {
		return service.hello();
	}

	@RequestMapping("/fail")
	public String fail() {
		return service.fail();
	}

	public static void main(String[] args) {
		SpringApplication.run(HystrixOnlyApplication.class, args);
	}
}
