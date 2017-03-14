/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.ribbon;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/*
 * Configuration Properties to indicate which Ribbon configurations 
 * should be eagerly loaded up
 * 
 * @author Biju Kunjummen
 */
@ConfigurationProperties(prefix = "ribbon.eager-load")
public class RibbonEagerLoadProperties {
	private boolean enabled = false;
	private List<String> clients;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public List<String> getClients() {
		return clients;
	}

	public void setClients(List<String> clients) {
		this.clients = clients;
	}
}
