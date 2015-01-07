package org.springframework.netflix.hystrix.amqp;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = HystrixAmqpTests.Application.class)
@WebAppConfiguration
@IntegrationTest({ "server.port=0", "spring.jmx.enabled=true" })
@DirtiesContext
public class HystrixAmqpTests {

	@EnableAutoConfiguration
	@EnableDiscoveryClient
	@EnableCircuitBreaker
	@RestController
	public static class Application {

		@HystrixCommand
		@RequestMapping("/")
		public String hello() {
			return "Hello World";
		}

		public static void main(String[] args) {
			SpringApplication.run(Application.class, args);
		}
	}

	@Test
	public void contextLoads() { }
}
