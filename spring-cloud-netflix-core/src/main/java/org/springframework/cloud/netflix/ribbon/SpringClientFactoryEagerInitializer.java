package org.springframework.cloud.netflix.ribbon;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

/**
 * Responsible for eagerly creating the Ribbon Spring Application contexts for the
 * registered named clients in {@link SpringClientFactory}
 * 
 * @author Biju Kunjummen
 */
class SpringClientFactoryEagerInitializer
		implements ApplicationListener<ApplicationReadyEvent> {

	private final SpringClientFactory springClientFactory;

	public SpringClientFactoryEagerInitializer(SpringClientFactory springClientFactory) {
		this.springClientFactory = springClientFactory;
	}

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		this.springClientFactory.createAndCacheContexts();
	}
}
