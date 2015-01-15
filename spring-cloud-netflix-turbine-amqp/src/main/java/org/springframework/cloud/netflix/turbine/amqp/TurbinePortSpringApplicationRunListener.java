package org.springframework.cloud.netflix.turbine.amqp;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.MapPropertySource;

public class TurbinePortSpringApplicationRunListener implements
		ApplicationListener<ApplicationEnvironmentPreparedEvent> {

	@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
		Integer serverPort = event.getEnvironment().getProperty("server.port",
				Integer.class);
		Integer managementPort = event.getEnvironment().getProperty("management.port",
				Integer.class);
		Integer turbinePort = event.getEnvironment().getProperty("turbine.amqp.port",
				Integer.class);
		if (serverPort == null && managementPort == null) {
			return;
		}
		if (serverPort != -1) {
			Map<String, Object> ports = new HashMap<String, Object>();
			ports.put("server.port", -1);
			if (serverPort != null && turbinePort==null) {
				ports.put("turbine.amqp.port", serverPort);
			}
			event.getEnvironment().getPropertySources()
					.addFirst(new MapPropertySource("ports", ports));
		}
	}

}
