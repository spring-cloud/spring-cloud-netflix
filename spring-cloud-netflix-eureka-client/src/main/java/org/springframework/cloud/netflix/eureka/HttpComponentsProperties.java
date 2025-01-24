/*
 * Copyright 2013-2025 the original author or authors.
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

import com.netflix.discovery.shared.transport.EurekaHttpClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.client.RestClient;

/**
 * Properties for configuring Apache HttpComponents used in {@link RestClient} required by
 * {@link EurekaHttpClient}.
 *
 * @author Max Brauer
 * @since 4.3.0
 */
@ConfigurationProperties("eureka.client.http-components")
public class HttpComponentsProperties {

	private Boolean enableProtocolUpgrades;

	public Boolean isEnableProtocolUpgrades() {
		return enableProtocolUpgrades;
	}

	public void setEnableProtocolUpgrades(Boolean enableProtocolUpgrades) {
		this.enableProtocolUpgrades = enableProtocolUpgrades;
	}

	@Override
	public String toString() {
		return "HttpComponents5Properties{" + "enableProtocolUpgrades=" + enableProtocolUpgrades + '}';
	}

}
