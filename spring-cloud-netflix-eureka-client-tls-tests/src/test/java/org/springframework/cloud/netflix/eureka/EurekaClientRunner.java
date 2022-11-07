/*
 * Copyright 2018-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.eureka;

import java.io.File;
import java.util.function.BooleanSupplier;

import com.netflix.discovery.AbstractDiscoveryClientOptionalArgs;

import org.springframework.cloud.client.discovery.DiscoveryClient;

public class EurekaClientRunner extends AppRunner {

	public EurekaClientRunner(Class<?> appClass, AppRunner server) {
		super(appClass);

		property("eureka.client.registerWithEureka", "false");
		property("eureka.client.fetchRegistry", "true");
		property("eureka.client.serviceUrl.defaultZone", server.root() + "eureka/");
		property("eureka.client.refresh.enable", "true");
	}

	public EurekaClientRunner(Class<?> appClass, AppRunner server, String service) {
		this(appClass, server);
		property("eureka.client.registerWithEureka", "true");
		property("spring.application.name", service);
	}

	public void enableTls() {
		property("eureka.client.tls.enabled", "true");
	}

	public void disableTls() {
		property("eureka.client.tls.enabled", "false");
	}

	public void setKeyStore(File keyStore, String keyStorePassword, String keyPassword) {
		property("eureka.client.tls.key-store", pathOf(keyStore));
		property("eureka.client.tls.key-store-password", keyStorePassword);
		property("eureka.client.tls.key-password", keyPassword);
	}

	public void setKeyStore(File keyStore) {
		property("eureka.client.tls.key-store", pathOf(keyStore));
	}

	public void setTrustStore(File trustStore, String password) {
		property("eureka.client.tls.trust-store", pathOf(trustStore));
		property("eureka.client.tls.trust-store-password", password);
	}

	public void setTrustStore(File trustStore) {
		property("eureka.client.tls.trust-store", pathOf(trustStore));
	}

	private String pathOf(File file) {
		return String.format("file:%s", file.getAbsolutePath());
	}

	public void waitServiceViaEureka(int seconds) {
		assertInSeconds(this::foundServiceViaEureka, seconds);
	}

	private void assertInSeconds(BooleanSupplier assertion, int seconds) {
		long start = System.currentTimeMillis();
		long limit = 1000L * seconds;
		long duration;

		do {
			if (assertion.getAsBoolean()) {
				return;
			}
			duration = System.currentTimeMillis() - start;
			Thread.yield();

		}
		while (duration < limit);

		throw new RuntimeException();
	}

	public boolean foundServiceViaEureka() {
		DiscoveryClient discovery = getBean(DiscoveryClient.class);
		return !discovery.getServices().isEmpty();
	}

	@SuppressWarnings("unchecked")
	public AbstractDiscoveryClientOptionalArgs<Void> discoveryClientOptionalArgs() {
		return getBean(AbstractDiscoveryClientOptionalArgs.class);
	}

}
