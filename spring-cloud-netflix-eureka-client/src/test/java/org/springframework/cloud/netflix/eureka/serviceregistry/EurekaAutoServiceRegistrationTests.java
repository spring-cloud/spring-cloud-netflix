/*
 * Copyright 2026-present the original author or authors.
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

package org.springframework.cloud.netflix.eureka.serviceregistry;

import org.junit.jupiter.api.Test;

import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextClosedEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EurekaAutoServiceRegistrationTests {

	private final ApplicationContext context = mock(ApplicationContext.class);

	private final EurekaServiceRegistry registry = mock(EurekaServiceRegistry.class);

	private final EurekaRegistration registration = mock(EurekaRegistration.class);

	private final EurekaAutoServiceRegistration autoRegistration = new EurekaAutoServiceRegistration(this.context,
			this.registry, this.registration);

	@Test
	void registersWithEmbeddedServerPort() {
		when(this.registration.getNonSecurePort()).thenReturn(0, 9090);
		this.autoRegistration.onApplicationEvent(event(null, 9090));
		verify(this.registration).setNonSecurePort(9090);
		verify(this.registry).register(this.registration);
		assertThat(this.autoRegistration.isRunning()).isTrue();
	}

	@Test
	void ignoresManagementServerPort() {
		this.autoRegistration.onApplicationEvent(event("management", 9091));
		verify(this.registration, never()).setNonSecurePort(9091);
		verify(this.registry, never()).register(this.registration);
		assertThat(this.autoRegistration.isRunning()).isFalse();
	}

	@Test
	void preservesConfiguredPort() {
		when(this.registration.getNonSecurePort()).thenReturn(8080);
		this.autoRegistration.onApplicationEvent(event(null, 9090));
		verify(this.registration, never()).setNonSecurePort(9090);
		verify(this.registry).register(this.registration);
	}

	@Test
	void deregistersOnlyWhenOwnContextCloses() {
		assertThat(this.autoRegistration.supportsEventType(WebServerInitializedEvent.class)).isTrue();
		assertThat(this.autoRegistration.supportsEventType(ContextClosedEvent.class)).isTrue();
		this.autoRegistration.onApplicationEvent(new ContextClosedEvent(mock(ApplicationContext.class)));
		verify(this.registry, never()).deregister(this.registration);
		this.autoRegistration.onApplicationEvent(new ContextClosedEvent(this.context));
		verify(this.registry).deregister(this.registration);
	}

	private WebServerInitializedEvent event(String namespace, int port) {
		WebServerApplicationContext serverContext = mock(WebServerApplicationContext.class);
		when(serverContext.getServerNamespace()).thenReturn(namespace);
		WebServer server = mock(WebServer.class);
		when(server.getPort()).thenReturn(port);
		return new WebServerInitializedEvent(server) {
			@Override
			public WebServerApplicationContext getApplicationContext() {
				return serverContext;
			}
		};
	}

}
