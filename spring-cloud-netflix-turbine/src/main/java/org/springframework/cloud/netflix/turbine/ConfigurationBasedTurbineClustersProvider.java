/*
 * Copyright 2013-2017 the original author or authors.
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

package org.springframework.cloud.netflix.turbine;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

/**
 * Provides clusters names for Turbine based on configuration value.
 *
 * @author Anastasiia Smirnova
 */
public class ConfigurationBasedTurbineClustersProvider implements TurbineClustersProvider {

	private static final Log log = LogFactory.getLog(ConfigurationBasedTurbineClustersProvider.class);
	private final TurbineAggregatorProperties properties;

	public ConfigurationBasedTurbineClustersProvider(TurbineAggregatorProperties turbineAggregatorProperties) {
		this.properties = turbineAggregatorProperties;
	}

	@Override
	public List<String> getClusterNames() {
		List<String> clusterNames = properties.getClusterConfig();
		log.trace("Using clusters names: " + clusterNames);
		return clusterNames;
	}
}
