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
package org.springframework.cloud.netflix.eureka.server;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Configuration properties for the Eureka dashboard (UI). 
 * @author Dave Syer
 *
 */
@ConfigurationProperties("eureka.dashboard")
@Data
public class EurekaDashboardProperties {
	
	/**
	 * The path to the Eureka dashboard (relative to the servlet path). Defaults to "/".
	 */
	private String path = "/";
	
	/**
	 * FLag to enable the Eureka dashboard. Default true.
	 */
	private boolean enabled = true;

}
