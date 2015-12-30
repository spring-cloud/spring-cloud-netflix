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

package org.springframework.cloud.netflix.eureka;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.PropertyResolver;

import com.netflix.appinfo.EurekaAccept;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.transport.EurekaTransportConfig;

import lombok.Data;

/**
 * @author Dave Syer
 */
@Data
@ConfigurationProperties(EurekaClientConfigBean.PREFIX)
public class EurekaClientConfigBean implements EurekaClientConfig, EurekaConstants {

	public static final String PREFIX = "eureka.client";

	@Autowired(required = false)
	PropertyResolver propertyResolver;

	public static final String DEFAULT_URL = "http://localhost:8761" + DEFAULT_PREFIX
			+ "/";

	public static final String DEFAULT_ZONE = "defaultZone";

	private static final int MINUTES = 60;

	/**
	 * Flag to indicate that the Eureka client is enabled.
	 */
	private boolean enabled = true;

	private EurekaTransportConfig transport = new CloudEurkeaTransportConfig();

	private int registryFetchIntervalSeconds = 30;

	private int instanceInfoReplicationIntervalSeconds = 30;

	private int initialInstanceInfoReplicationIntervalSeconds = 40;

	private int eurekaServiceUrlPollIntervalSeconds = 5 * MINUTES;

	private String proxyPort;

	private String proxyHost;

	private String proxyUserName;

	private String proxyPassword;

	private int eurekaServerReadTimeoutSeconds = 8;

	private int eurekaServerConnectTimeoutSeconds = 5;

	private String backupRegistryImpl;

	private int eurekaServerTotalConnections = 200;

	private int eurekaServerTotalConnectionsPerHost = 50;

	private String eurekaServerURLContext;

	private String eurekaServerPort;

	private String eurekaServerDNSName;

	private String region = "us-east-1";

	private int eurekaConnectionIdleTimeoutSeconds = 30;

	private String registryRefreshSingleVipAddress;

	private int heartbeatExecutorThreadPoolSize = 2;

	private int heartbeatExecutorExponentialBackOffBound = 10;

	private int cacheRefreshExecutorThreadPoolSize = 2;

	private int cacheRefreshExecutorExponentialBackOffBound = 10;

	private Map<String, String> serviceUrl = new HashMap<>();

	{
		this.serviceUrl.put(DEFAULT_ZONE, DEFAULT_URL);
	}

	private boolean gZipContent = true;

	private boolean useDnsForFetchingServiceUrls = false;

	private boolean registerWithEureka = true;

	private boolean preferSameZoneEureka = true;

	private boolean logDeltaDiff;

	private boolean disableDelta;

	private String fetchRemoteRegionsRegistry;

	private Map<String, String> availabilityZones = new HashMap<>();

	private boolean filterOnlyUpInstances = true;

	private boolean fetchRegistry = true;

	private String dollarReplacement = "_-";

	private String escapeCharReplacement = "__";

	private boolean allowRedirects = false;

	private boolean onDemandUpdateStatusChange = true;

	private String encoderName;

	private String decoderName;

	private String clientDataAccept = EurekaAccept.full.name();

	@Override
	public boolean shouldGZipContent() {
		return this.gZipContent;
	}

	@Override
	public boolean shouldUseDnsForFetchingServiceUrls() {
		return this.useDnsForFetchingServiceUrls;
	}

	@Override
	public boolean shouldRegisterWithEureka() {
		return this.registerWithEureka;
	}

	@Override
	public boolean shouldPreferSameZoneEureka() {
		return this.preferSameZoneEureka;
	}

	@Override
	public boolean shouldLogDeltaDiff() {
		return this.logDeltaDiff;
	}

	@Override
	public boolean shouldDisableDelta() {
		return this.disableDelta;
	}

	@Override
	public String fetchRegistryForRemoteRegions() {
		return this.fetchRemoteRegionsRegistry;
	}

	@Override
	public String[] getAvailabilityZones(String region) {
		String value = this.availabilityZones.get(region);
		if (value == null) {
			value = DEFAULT_ZONE;
		}
		return value.split(",");
	}

	@Override
	public List<String> getEurekaServerServiceUrls(String myZone) {
		String serviceUrls = this.serviceUrl.get(myZone);
		if (serviceUrls == null || serviceUrls.isEmpty()) {
			serviceUrls = this.serviceUrl.get(DEFAULT_ZONE);
		}
		if (serviceUrls != null) {
			return Arrays.asList(serviceUrls.split(","));
		}

		return new ArrayList<>();
	}

	@Override
	public boolean shouldFilterOnlyUpInstances() {
		return this.filterOnlyUpInstances;
	}

	@Override
	public boolean shouldFetchRegistry() {
		return this.fetchRegistry;
	}

	@Override
	public boolean allowRedirects() {
		return this.allowRedirects;
	}

	@Override
	public boolean shouldOnDemandUpdateStatusChange() {
		return this.onDemandUpdateStatusChange;
	}

	@Override
	public String getExperimental(String name) {
		if (this.propertyResolver != null) {
			return this.propertyResolver.getProperty(PREFIX + ".experimental." + name,
					String.class, null);
		}
		return null;
	}

	@Override
	public EurekaTransportConfig getTransportConfig() {
		return getTransport();
	}
}
