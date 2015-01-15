/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.cloud.netflix.archaius;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.EnvironmentConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;

import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.ConfigurationManager;

/**
 * @author Dave Syer
 */
public class ArchaiusEndpoint extends AbstractEndpoint<Map<String, Object>> {

	public ArchaiusEndpoint() {
		super("archaius");
	}

	@Override
	public Map<String, Object> invoke() {
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		AbstractConfiguration config = ConfigurationManager.getConfigInstance();
		if (config instanceof ConcurrentCompositeConfiguration) {
			ConcurrentCompositeConfiguration composite = (ConcurrentCompositeConfiguration) config;
			for (Configuration item : composite.getConfigurations()) {
				append(map, item);
			}
		}
		else {
			append(map, config);
		}
		return map;
	}

	private void append(Map<String, Object> map, Configuration config) {
		if (config instanceof ConfigurableEnvironmentConfiguration) {
			return;
		}
		if (config instanceof SystemConfiguration) {
			return;
		}
		if (config instanceof EnvironmentConfiguration) {
			return;
		}
		for (Iterator<String> iter = config.getKeys(); iter.hasNext();) {
			String key = iter.next();
			map.put(key, config.getProperty(key));
		}
	}

}
