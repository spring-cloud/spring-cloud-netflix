/*
 * Copyright 2013-present the original author or authors.
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

package org.springframework.cloud.netflix.eureka.server;

import java.io.IOException;
import java.security.GeneralSecurityException;

import com.netflix.discovery.AbstractDiscoveryClientOptionalArgs;
import com.netflix.discovery.Jersey3DiscoveryClientOptionalArgs;
import com.netflix.discovery.shared.transport.jersey.TransportClientFactories;
import com.netflix.discovery.shared.transport.jersey3.Jersey3TransportClientFactories;
import com.netflix.eureka.transport.EurekaServerHttpClientFactory;
import com.netflix.eureka.transport.Jersey3EurekaServerHttpClientFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.configuration.SSLContextFactory;
import org.springframework.cloud.configuration.TlsProperties;
import org.springframework.context.annotation.Bean;

/**
 * @author Olga Maciaszek-Sharma
 */
@AutoConfiguration
@ConditionalOnBean(EurekaServerMarkerConfiguration.Marker.class)
public class EurekaServerJerseyClientAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(AbstractDiscoveryClientOptionalArgs.class)
	public Jersey3DiscoveryClientOptionalArgs jersey3DiscoveryClientOptionalArgs(
			@Autowired(required = false) TlsProperties tlsProperties) throws GeneralSecurityException, IOException {
		Jersey3DiscoveryClientOptionalArgs optionalArgs = new Jersey3DiscoveryClientOptionalArgs();
		if (tlsProperties != null && tlsProperties.isEnabled()) {
			SSLContextFactory factory = new SSLContextFactory(tlsProperties);
			optionalArgs.setSSLContext(factory.createSSLContext());
		}
		return optionalArgs;
	}

	@Bean
	@ConditionalOnMissingBean(TransportClientFactories.class)
	public Jersey3TransportClientFactories jersey3TransportClientFactories() {
		return Jersey3TransportClientFactories.getInstance();
	}

	@Bean
	@ConditionalOnMissingBean(EurekaServerHttpClientFactory.class)
	public Jersey3EurekaServerHttpClientFactory jersey3EurekaServerHttpClientFactory() {
		return new Jersey3EurekaServerHttpClientFactory();
	}

}
