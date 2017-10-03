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
package org.springframework.cloud.netflix.hystrix.dashboard;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Roy Clarkson
 * @author Fahim Farook
 */
@ConfigurationProperties("hystrix.dashboard")
public class HystrixDashboardProperties {

	/**
	 * Directs the Hystrix dashboard to ignore 'Connection:close' headers if present in
	 * the Hystrix response stream
	 */
	private boolean enableIgnoreConnectionCloseHeader = false;
	
	/**
	 * Initialization parameters for {@link ProxyStreamServlet}. ProxyStreamServlet itself
	 * is not dependent on any initialization parameters, but could be used for adding web
	 * container specific configurations. i.e. wl-dispatch-policy for WebLogic.
	 */
	private Map<String, String> initParameters = new HashMap<>();

	public boolean isEnableIgnoreConnectionCloseHeader() {
		return enableIgnoreConnectionCloseHeader;
	}

	public void setEnableIgnoreConnectionCloseHeader(
			boolean enableIgnoreConnectionCloseHeader) {
		this.enableIgnoreConnectionCloseHeader = enableIgnoreConnectionCloseHeader;
	}

	public Map<String, String> getInitParameters() {
		return this.initParameters;
	}
}
