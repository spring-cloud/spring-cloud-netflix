/*
 * Copyright 2013-2015 the original author or authors.
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

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * @author Spencer Gibb
 */
@ConfigurationProperties("turbine")
public class TurbineProperties {

	private String clusterNameExpression;

	private String appConfig;

	private boolean combineHostPort = false;

	public TurbineProperties() {
	}

	public List<String> getAppConfigList() {
		if (!StringUtils.hasText(appConfig)) {
			return null;
		}
		String[] parts = appConfig.trim().split(",");
		if (parts != null && parts.length > 0) {
			return Arrays.asList(parts);
		}
		return null;
	}

	public String getClusterNameExpression() {
		return this.clusterNameExpression;
	}

	public String getAppConfig() {
		return this.appConfig;
	}

	public boolean isCombineHostPort() {
		return this.combineHostPort;
	}

	public void setClusterNameExpression(String clusterNameExpression) {
		this.clusterNameExpression = clusterNameExpression;
	}

	public void setAppConfig(String appConfig) {
		this.appConfig = appConfig;
	}

	public void setCombineHostPort(boolean combineHostPort) {
		this.combineHostPort = combineHostPort;
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof TurbineProperties))
			return false;
		final TurbineProperties other = (TurbineProperties) o;
		if (!other.canEqual((Object) this))
			return false;
		final Object this$clusterNameExpression = this.clusterNameExpression;
		final Object other$clusterNameExpression = other.clusterNameExpression;
		if (this$clusterNameExpression == null ?
				other$clusterNameExpression != null :
				!this$clusterNameExpression.equals(other$clusterNameExpression))
			return false;
		final Object this$appConfig = this.appConfig;
		final Object other$appConfig = other.appConfig;
		if (this$appConfig == null ?
				other$appConfig != null :
				!this$appConfig.equals(other$appConfig))
			return false;
		if (this.combineHostPort != other.combineHostPort)
			return false;
		return true;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $clusterNameExpression = this.clusterNameExpression;
		result = result * PRIME + ($clusterNameExpression == null ?
				0 :
				$clusterNameExpression.hashCode());
		final Object $appConfig = this.appConfig;
		result = result * PRIME + ($appConfig == null ? 0 : $appConfig.hashCode());
		result = result * PRIME + (this.combineHostPort ? 79 : 97);
		return result;
	}

	protected boolean canEqual(Object other) {
		return other instanceof TurbineProperties;
	}

	public String toString() {
		return "org.springframework.cloud.netflix.turbine.TurbineProperties(clusterNameExpression="
				+ this.clusterNameExpression + ", appConfig=" + this.appConfig
				+ ", combineHostPort=" + this.combineHostPort + ")";
	}
}
