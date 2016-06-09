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

import lombok.Data;

/**
 * @author Spencer Gibb
 */
@Data
@ConfigurationProperties("turbine")
public class TurbineProperties {

	private String clusterNameExpression;

	private String appConfig;

	private boolean combineHostPort = true;

	public List<String> getAppConfigList() {
		if (!StringUtils.hasText(this.appConfig)) {
			return null;
		}
		String[] parts = StringUtils.commaDelimitedListToStringArray(this.appConfig);
		if (parts != null && parts.length > 0) {
			parts = StringUtils.trimArrayElements(parts);
			return Arrays.asList(parts);
		}
		return null;
	}
}
