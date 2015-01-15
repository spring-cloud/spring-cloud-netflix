package org.springframework.cloud.netflix.turbine.amqp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TurbineAmqpTests.Application.class)
@WebAppConfiguration
@IntegrationTest({ "server.port=0", "turbine.amqp.port=0", "spring.jmx.enabled=true" })
public class TurbineAmqpTests {

	@EnableAutoConfiguration
	@EnableTurbineAmqp
	public static class Application {
		public static void main(String[] args) {
			new SpringApplicationBuilder()
					.sources(Application.class)
					.run(args);
		}
	}

	@Test
	public void contextLoads() { }
}
