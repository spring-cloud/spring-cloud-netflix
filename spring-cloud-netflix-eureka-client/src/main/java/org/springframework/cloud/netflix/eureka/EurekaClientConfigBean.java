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
import org.springframework.util.StringUtils;

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

	private EurekaTransportConfig transport = new CloudEurekaTransportConfig();

	/**
	 * Indicates how often(in seconds) to fetch the registry information from the eureka
	 * server.
	 */
	private int registryFetchIntervalSeconds = 30;

	/**
	 * Indicates how often(in seconds) to replicate instance changes to be replicated to
	 * the eureka server.
	 */
	private int instanceInfoReplicationIntervalSeconds = 30;

	/**
	 * Indicates how long initially (in seconds) to replicate instance info to the eureka
	 * server
	 */
	private int initialInstanceInfoReplicationIntervalSeconds = 40;

	/**
	 * Indicates how often(in seconds) to poll for changes to eureka server information.
	 * Eureka servers could be added or removed and this setting controls how soon the
	 * eureka clients should know about it.
	 */
	private int eurekaServiceUrlPollIntervalSeconds = 5 * MINUTES;

	/**
	 * Gets the proxy port to eureka server if any.
	 */
	private String proxyPort;

	/**
	 * Gets the proxy host to eureka server if any.
	 */
	private String proxyHost;

	/**
	 * Gets the proxy user name if any.
	 */
	private String proxyUserName;

	/**
	 * Gets the proxy password if any.
	 */
	private String proxyPassword;

	/**
	 * Indicates how long to wait (in seconds) before a read from eureka server needs to
	 * timeout.
	 */
	private int eurekaServerReadTimeoutSeconds = 8;

	/**
	 * Indicates how long to wait (in seconds) before a connection to eureka server needs
	 * to timeout. Note that the connections in the client are pooled by
	 * org.apache.http.client.HttpClient and this setting affects the actual connection
	 * creation and also the wait time to get the connection from the pool.
	 */
	private int eurekaServerConnectTimeoutSeconds = 5;

	/**
	 * Gets the name of the implementation which implements BackupRegistry to fetch the
	 * registry information as a fall back option for only the first time when the eureka
	 * client starts.
	 *
	 * This may be needed for applications which needs additional resiliency for registry
	 * information without which it cannot operate.
	 */
	private String backupRegistryImpl;

	/**
	 * Gets the total number of connections that is allowed from eureka client to all
	 * eureka servers.
	 */
	private int eurekaServerTotalConnections = 200;

	/**
	 * Gets the total number of connections that is allowed from eureka client to a eureka
	 * server host.
	 */
	private int eurekaServerTotalConnectionsPerHost = 50;

	/**
	 * Gets the URL context to be used to construct the service url to contact eureka
	 * server when the list of eureka servers come from the DNS. This information is not
	 * required if the contract returns the service urls from eurekaServerServiceUrls.
	 *
	 * The DNS mechanism is used when useDnsForFetchingServiceUrls is set to true and the
	 * eureka client expects the DNS to configured a certain way so that it can fetch
	 * changing eureka servers dynamically. The changes are effective at runtime.
	 */
	private String eurekaServerURLContext;

	/**
	 * Gets the port to be used to construct the service url to contact eureka server when
	 * the list of eureka servers come from the DNS.This information is not required if
	 * the contract returns the service urls eurekaServerServiceUrls(String).
	 *
	 * The DNS mechanism is used when useDnsForFetchingServiceUrls is set to true and the
	 * eureka client expects the DNS to configured a certain way so that it can fetch
	 * changing eureka servers dynamically.
	 *
	 * The changes are effective at runtime.
	 */
	private String eurekaServerPort;

	/**
	 * Gets the DNS name to be queried to get the list of eureka servers.This information
	 * is not required if the contract returns the service urls by implementing
	 * serviceUrls.
	 *
	 * The DNS mechanism is used when useDnsForFetchingServiceUrls is set to true and the
	 * eureka client expects the DNS to configured a certain way so that it can fetch
	 * changing eureka servers dynamically.
	 *
	 * The changes are effective at runtime.
	 */
	private String eurekaServerDNSName;

	/**
	 * Gets the region (used in AWS datacenters) where this instance resides.
	 */
	private String region = "us-east-1";

	/**
	 * Indicates how much time (in seconds) that the HTTP connections to eureka server can
	 * stay idle before it can be closed.
	 *
	 * In the AWS environment, it is recommended that the values is 30 seconds or less,
	 * since the firewall cleans up the connection information after a few mins leaving
	 * the connection hanging in limbo
	 */
	private int eurekaConnectionIdleTimeoutSeconds = 30;

	/**
	 * Indicates whether the client is only interested in the registry information for a
	 * single VIP.
	 */
	private String registryRefreshSingleVipAddress;

	/**
	 * The thread pool size for the heartbeatExecutor to initialise with
	 */
	private int heartbeatExecutorThreadPoolSize = 2;

	/**
	 * Heartbeat executor exponential back off related property. It is a maximum
	 * multiplier value for retry delay, in case where a sequence of timeouts occurred.
	 */
	private int heartbeatExecutorExponentialBackOffBound = 10;

	/**
	 * The thread pool size for the cacheRefreshExecutor to initialise with
	 */
	private int cacheRefreshExecutorThreadPoolSize = 2;

	/**
	 * Cache refresh executor exponential back off related property. It is a maximum
	 * multiplier value for retry delay, in case where a sequence of timeouts occurred.
	 */
	private int cacheRefreshExecutorExponentialBackOffBound = 10;

	/**
	 * Map of availability zone to list of fully qualified URLs to communicate with eureka
	 * server. Each value can be a single URL or a comma separated list of alternative
	 * locations.
	 *
	 * Typically the eureka server URLs carry protocol,host,port,context and version
	 * information if any. Example:
	 * http://ec2-256-156-243-129.compute-1.amazonaws.com:7001/eureka/
	 *
	 * The changes are effective at runtime at the next service url refresh cycle as
	 * specified by eurekaServiceUrlPollIntervalSeconds.
	 */
	private Map<String, String> serviceUrl = new HashMap<>();

	{
		this.serviceUrl.put(DEFAULT_ZONE, DEFAULT_URL);
	}

	/**
	 * Indicates whether the content fetched from eureka server has to be compressed
	 * whenever it is supported by the server. The registry information from the eureka
	 * server is compressed for optimum network traffic.
	 */
	private boolean gZipContent = true;

	/**
	 * Indicates whether the eureka client should use the DNS mechanism to fetch a list of
	 * eureka servers to talk to. When the DNS name is updated to have additional servers,
	 * that information is used immediately after the eureka client polls for that
	 * information as specified in eurekaServiceUrlPollIntervalSeconds.
	 *
	 * Alternatively, the service urls can be returned serviceUrls, but the users should
	 * implement their own mechanism to return the updated list in case of changes.
	 *
	 * The changes are effective at runtime.
	 */
	private boolean useDnsForFetchingServiceUrls = false;

	/**
	 * Indicates whether or not this instance should register its information with eureka
	 * server for discovery by others.
	 *
	 * In some cases, you do not want your instances to be discovered whereas you just
	 * want do discover other instances.
	 */
	private boolean registerWithEureka = true;

	/**
	 * Indicates whether or not this instance should try to use the eureka server in the
	 * same zone for latency and/or other reason.
	 *
	 * Ideally eureka clients are configured to talk to servers in the same zone
	 *
	 * The changes are effective at runtime at the next registry fetch cycle as specified
	 * by registryFetchIntervalSeconds
	 */
	private boolean preferSameZoneEureka = true;

	/**
	 * Indicates whether to log differences between the eureka server and the eureka
	 * client in terms of registry information.
	 *
	 * Eureka client tries to retrieve only delta changes from eureka server to minimize
	 * network traffic. After receiving the deltas, eureka client reconciles the
	 * information from the server to verify it has not missed out some information.
	 * Reconciliation failures could happen when the client has had network issues
	 * communicating to server.If the reconciliation fails, eureka client gets the full
	 * registry information.
	 *
	 * While getting the full registry information, the eureka client can log the
	 * differences between the client and the server and this setting controls that.
	 *
	 * The changes are effective at runtime at the next registry fetch cycle as specified
	 * by registryFetchIntervalSecondsr
	 */
	private boolean logDeltaDiff;

	/**
	 * Indicates whether the eureka client should disable fetching of delta and should
	 * rather resort to getting the full registry information.
	 *
	 * Note that the delta fetches can reduce the traffic tremendously, because the rate
	 * of change with the eureka server is normally much lower than the rate of fetches.
	 *
	 * The changes are effective at runtime at the next registry fetch cycle as specified
	 * by registryFetchIntervalSeconds
	 */
	private boolean disableDelta;

	/**
	 * Comma separated list of regions for which the eureka registry information will be
	 * fetched. It is mandatory to define the availability zones for each of these regions
	 * as returned by availabilityZones. Failing to do so, will result in failure of
	 * discovery client startup.
	 *
	 */
	private String fetchRemoteRegionsRegistry;

	/**
	 * Gets the list of availability zones (used in AWS data centers) for the region in
	 * which this instance resides.
	 *
	 * The changes are effective at runtime at the next registry fetch cycle as specified
	 * by registryFetchIntervalSeconds.
	 */
	private Map<String, String> availabilityZones = new HashMap<>();

	/**
	 * Indicates whether to get the applications after filtering the applications for
	 * instances with only InstanceStatus UP states.
	 */
	private boolean filterOnlyUpInstances = true;

	/**
	 * Indicates whether this client should fetch eureka registry information from eureka
	 * server.
	 */
	private boolean fetchRegistry = true;

	/**
	 * Get a replacement string for Dollar sign <code>$</code> during
	 * serializing/deserializing information in eureka server.
	 */
	private String dollarReplacement = "_-";

	/**
	 * Get a replacement string for underscore sign <code>_</code> during
	 * serializing/deserializing information in eureka server.
	 */
	private String escapeCharReplacement = "__";

	/**
	 * Indicates whether server can redirect a client request to a backup server/cluster.
	 * If set to false, the server will handle the request directly, If set to true, it
	 * may send HTTP redirect to the client, with a new server location.
	 */
	private boolean allowRedirects = false;

	/**
	 * If set to true, local status updates via ApplicationInfoManager will trigger
	 * on-demand (but rate limited) register/updates to remote eureka servers
	 */
	private boolean onDemandUpdateStatusChange = true;

	/**
	 * This is a transient config and once the latest codecs are stable, can be removed
	 * (as there will only be one)
	 */
	private String encoderName;

	/**
	 * This is a transient config and once the latest codecs are stable, can be removed
	 * (as there will only be one)
	 */
	private String decoderName;

	/**
	 * EurekaAccept name for client data accept
	 */
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
		if (!StringUtils.isEmpty(serviceUrls)) {
			final String[] serviceUrlsSplit = StringUtils.commaDelimitedListToStringArray(serviceUrls);
			List<String> eurekaServiceUrls = new ArrayList<>(serviceUrlsSplit.length);
			for (String eurekaServiceUrl : serviceUrlsSplit) {
				if (!endsWithSlash(eurekaServiceUrl)) {
					eurekaServiceUrl += "/";
				}
				eurekaServiceUrls.add(eurekaServiceUrl);
			}
			return eurekaServiceUrls;
		}

		return new ArrayList<>();
	}

	private boolean endsWithSlash(String url) {
		return url.endsWith("/");
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
