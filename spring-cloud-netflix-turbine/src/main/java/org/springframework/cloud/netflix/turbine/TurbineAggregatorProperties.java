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

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Anastasiia Smirnova
 */
@ConfigurationProperties("turbine.aggregator")
public class TurbineAggregatorProperties {

	private static final String DEFAULT = "default";
	/**
	 * The list of cluster names.
	 */
	private List<String> clusterConfig = Collections.singletonList(DEFAULT);

	public List<String> getClusterConfig() {
		return clusterConfig;
	}

	public void setClusterConfig(List<String> clusterConfig) {
		this.clusterConfig = clusterConfig;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		TurbineAggregatorProperties that = (TurbineAggregatorProperties) o;
		return Objects.equals(clusterConfig, that.clusterConfig);
	}

	@Override
	public int hashCode() {
		return Objects.hash(clusterConfig);
	}

	@Override
	public String toString() {
		return "TurbineAggregatorProperties{" + "clusterConfig='" + clusterConfig + '\''
				+ '}';
	}
}
