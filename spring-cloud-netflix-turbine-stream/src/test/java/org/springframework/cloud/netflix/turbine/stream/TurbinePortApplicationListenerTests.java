package org.springframework.cloud.netflix.turbine.stream;

import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

public class TurbinePortApplicationListenerTests {
	
	private TurbinePortApplicationListener listener = new TurbinePortApplicationListener();
	
	private ConfigurableEnvironment environment = new StandardEnvironment();
	
	private ApplicationEnvironmentPreparedEvent event = new ApplicationEnvironmentPreparedEvent(new SpringApplication(), null, environment);

	@Test
	public void noop() {
		listener.onApplicationEvent(event);
	}

	@Test
	public void serverPortOnly() {
		EnvironmentTestUtils.addEnvironment(environment, "server.port=9999");
		listener.onApplicationEvent(event);
		assertEquals("-1", environment.resolvePlaceholders("${server.port}"));
		assertEquals("9999", environment.resolvePlaceholders("${turbine.stream.port}"));
	}

	@Test
	public void turbinePortOnly() {
		EnvironmentTestUtils.addEnvironment(environment, "turbine.stream.port=9999");
		listener.onApplicationEvent(event);
		assertEquals("9999", environment.resolvePlaceholders("${turbine.stream.port}"));
		assertEquals("0", environment.resolvePlaceholders("${server.port:0}"));
	}

	@Test
	public void turbineAndManagementPorts() {
		EnvironmentTestUtils.addEnvironment(environment, "turbine.stream.port=9999", "management.port=9000");
		listener.onApplicationEvent(event);
		assertEquals("9999", environment.resolvePlaceholders("${turbine.stream.port}"));
		assertEquals("9000", environment.resolvePlaceholders("${server.port:0}"));
		assertEquals("9000", environment.resolvePlaceholders("${management.port:0}"));
	}

	@Test
	public void turbineAndServerPorts() {
		EnvironmentTestUtils.addEnvironment(environment, "turbine.stream.port=9999", "server.port=9000");
		listener.onApplicationEvent(event);
		assertEquals("9999", environment.resolvePlaceholders("${turbine.stream.port}"));
		assertEquals("9000", environment.resolvePlaceholders("${server.port:0}"));
		assertEquals("0", environment.resolvePlaceholders("${management.port:0}"));
	}

}
