package org.springframework.cloud.netflix.zuul;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.netflix.zuul.monitoring.CounterFactory;
import com.netflix.zuul.monitoring.TracerFactory;

import lombok.extern.apachecommons.CommonsLog;

@CommonsLog
public class ZuulMetricsInitializer implements ServletContextListener {

	private final CounterFactory counterFactory;
	private final TracerFactory tracerFactory;

	public ZuulMetricsInitializer(CounterFactory counterFactory,
			TracerFactory tracerFactory) {
		this.counterFactory = counterFactory;
		this.tracerFactory = tracerFactory;
	}

	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		log.info("Starting metrics initializer context listener");
		TracerFactory.initialize(tracerFactory);
		CounterFactory.initialize(counterFactory);
	}

	@Override
	public void contextDestroyed(ServletContextEvent servletContextEvent) {
	}
}
