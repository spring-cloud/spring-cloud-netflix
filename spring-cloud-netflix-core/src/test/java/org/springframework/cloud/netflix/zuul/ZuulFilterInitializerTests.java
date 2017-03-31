package org.springframework.cloud.netflix.zuul;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContextEvent;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.monitoring.CounterFactory;
import com.netflix.zuul.monitoring.TracerFactory;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class ZuulFilterInitializerTests {

	private static final ServletContextEvent DUMMY_SERVLET_CONTEXT_EVENT = mock(ServletContextEvent.class);
	private Map<String, ZuulFilter> filters = new HashMap<>();
	private CounterFactory counterFactory = mock(CounterFactory.class);
	private TracerFactory tracerFactory = mock(TracerFactory.class);
	private final ZuulFilterInitializer initializer = new ZuulFilterInitializer(filters, counterFactory, tracerFactory);

	@Test
	public void shouldInitializeMetricsFactoriesOnContextInitializedEvent() throws Exception {
		initializer.contextInitialized(DUMMY_SERVLET_CONTEXT_EVENT);

		assertEquals(tracerFactory, TracerFactory.instance());
		assertEquals(counterFactory, CounterFactory.instance());
	}
}