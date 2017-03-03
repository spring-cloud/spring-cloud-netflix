package org.springframework.cloud.netflix.ribbon;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import java.util.List;

/**
 * Responsible for eagerly creating the child application context holding the Ribbon
 * related configuration
 *
 * @author Biju Kunjummen
 */
public class RibbonApplicationContextInitializer
		implements ApplicationListener<ApplicationReadyEvent> {

	private final SpringClientFactory springClientFactory;
	private final List<String> serviceIds;

	public RibbonApplicationContextInitializer(SpringClientFactory springClientFactory,
			List<String> serviceIds) {
		this.springClientFactory = springClientFactory;
		this.serviceIds = serviceIds;
	}

	private void initialize() {
		if (serviceIds != null) {
			for (String serviceId : serviceIds) {
				this.springClientFactory.getContext(serviceId);
			}
		}
	}

//	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		initialize();
	}
}
