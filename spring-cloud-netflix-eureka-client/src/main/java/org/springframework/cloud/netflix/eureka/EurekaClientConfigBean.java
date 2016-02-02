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

import com.netflix.appinfo.EurekaAccept;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.transport.EurekaTransportConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.PropertyResolver;

/**
 * @author Dave Syer
 */
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
	 * Map of availability zone to fully qualified URLs to communicate with eureka server.
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

	public EurekaClientConfigBean() {
	}

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

	public PropertyResolver getPropertyResolver() {
		return this.propertyResolver;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public EurekaTransportConfig getTransport() {
		return this.transport;
	}

	public int getRegistryFetchIntervalSeconds() {
		return this.registryFetchIntervalSeconds;
	}

	public int getInstanceInfoReplicationIntervalSeconds() {
		return this.instanceInfoReplicationIntervalSeconds;
	}

	public int getInitialInstanceInfoReplicationIntervalSeconds() {
		return this.initialInstanceInfoReplicationIntervalSeconds;
	}

	public int getEurekaServiceUrlPollIntervalSeconds() {
		return this.eurekaServiceUrlPollIntervalSeconds;
	}

	public String getProxyPort() {
		return this.proxyPort;
	}

	public String getProxyHost() {
		return this.proxyHost;
	}

	public String getProxyUserName() {
		return this.proxyUserName;
	}

	public String getProxyPassword() {
		return this.proxyPassword;
	}

	public int getEurekaServerReadTimeoutSeconds() {
		return this.eurekaServerReadTimeoutSeconds;
	}

	public int getEurekaServerConnectTimeoutSeconds() {
		return this.eurekaServerConnectTimeoutSeconds;
	}

	public String getBackupRegistryImpl() {
		return this.backupRegistryImpl;
	}

	public int getEurekaServerTotalConnections() {
		return this.eurekaServerTotalConnections;
	}

	public int getEurekaServerTotalConnectionsPerHost() {
		return this.eurekaServerTotalConnectionsPerHost;
	}

	public String getEurekaServerURLContext() {
		return this.eurekaServerURLContext;
	}

	public String getEurekaServerPort() {
		return this.eurekaServerPort;
	}

	public String getEurekaServerDNSName() {
		return this.eurekaServerDNSName;
	}

	public String getRegion() {
		return this.region;
	}

	public int getEurekaConnectionIdleTimeoutSeconds() {
		return this.eurekaConnectionIdleTimeoutSeconds;
	}

	public String getRegistryRefreshSingleVipAddress() {
		return this.registryRefreshSingleVipAddress;
	}

	public int getHeartbeatExecutorThreadPoolSize() {
		return this.heartbeatExecutorThreadPoolSize;
	}

	public int getHeartbeatExecutorExponentialBackOffBound() {
		return this.heartbeatExecutorExponentialBackOffBound;
	}

	public int getCacheRefreshExecutorThreadPoolSize() {
		return this.cacheRefreshExecutorThreadPoolSize;
	}

	public int getCacheRefreshExecutorExponentialBackOffBound() {
		return this.cacheRefreshExecutorExponentialBackOffBound;
	}

	public Map<String, String> getServiceUrl() {
		return this.serviceUrl;
	}

	public boolean isGZipContent() {
		return this.gZipContent;
	}

	public boolean isUseDnsForFetchingServiceUrls() {
		return this.useDnsForFetchingServiceUrls;
	}

	public boolean isRegisterWithEureka() {
		return this.registerWithEureka;
	}

	public boolean isPreferSameZoneEureka() {
		return this.preferSameZoneEureka;
	}

	public boolean isLogDeltaDiff() {
		return this.logDeltaDiff;
	}

	public boolean isDisableDelta() {
		return this.disableDelta;
	}

	public String getFetchRemoteRegionsRegistry() {
		return this.fetchRemoteRegionsRegistry;
	}

	public Map<String, String> getAvailabilityZones() {
		return this.availabilityZones;
	}

	public boolean isFilterOnlyUpInstances() {
		return this.filterOnlyUpInstances;
	}

	public boolean isFetchRegistry() {
		return this.fetchRegistry;
	}

	public String getDollarReplacement() {
		return this.dollarReplacement;
	}

	public String getEscapeCharReplacement() {
		return this.escapeCharReplacement;
	}

	public boolean isAllowRedirects() {
		return this.allowRedirects;
	}

	public boolean isOnDemandUpdateStatusChange() {
		return this.onDemandUpdateStatusChange;
	}

	public String getEncoderName() {
		return this.encoderName;
	}

	public String getDecoderName() {
		return this.decoderName;
	}

	public String getClientDataAccept() {
		return this.clientDataAccept;
	}

	public void setPropertyResolver(PropertyResolver propertyResolver) {
		this.propertyResolver = propertyResolver;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setTransport(EurekaTransportConfig transport) {
		this.transport = transport;
	}

	public void setRegistryFetchIntervalSeconds(int registryFetchIntervalSeconds) {
		this.registryFetchIntervalSeconds = registryFetchIntervalSeconds;
	}

	public void setInstanceInfoReplicationIntervalSeconds(
			int instanceInfoReplicationIntervalSeconds) {
		this.instanceInfoReplicationIntervalSeconds = instanceInfoReplicationIntervalSeconds;
	}

	public void setInitialInstanceInfoReplicationIntervalSeconds(
			int initialInstanceInfoReplicationIntervalSeconds) {
		this.initialInstanceInfoReplicationIntervalSeconds = initialInstanceInfoReplicationIntervalSeconds;
	}

	public void setEurekaServiceUrlPollIntervalSeconds(
			int eurekaServiceUrlPollIntervalSeconds) {
		this.eurekaServiceUrlPollIntervalSeconds = eurekaServiceUrlPollIntervalSeconds;
	}

	public void setProxyPort(String proxyPort) {
		this.proxyPort = proxyPort;
	}

	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	public void setProxyUserName(String proxyUserName) {
		this.proxyUserName = proxyUserName;
	}

	public void setProxyPassword(String proxyPassword) {
		this.proxyPassword = proxyPassword;
	}

	public void setEurekaServerReadTimeoutSeconds(int eurekaServerReadTimeoutSeconds) {
		this.eurekaServerReadTimeoutSeconds = eurekaServerReadTimeoutSeconds;
	}

	public void setEurekaServerConnectTimeoutSeconds(
			int eurekaServerConnectTimeoutSeconds) {
		this.eurekaServerConnectTimeoutSeconds = eurekaServerConnectTimeoutSeconds;
	}

	public void setBackupRegistryImpl(String backupRegistryImpl) {
		this.backupRegistryImpl = backupRegistryImpl;
	}

	public void setEurekaServerTotalConnections(int eurekaServerTotalConnections) {
		this.eurekaServerTotalConnections = eurekaServerTotalConnections;
	}

	public void setEurekaServerTotalConnectionsPerHost(
			int eurekaServerTotalConnectionsPerHost) {
		this.eurekaServerTotalConnectionsPerHost = eurekaServerTotalConnectionsPerHost;
	}

	public void setEurekaServerURLContext(String eurekaServerURLContext) {
		this.eurekaServerURLContext = eurekaServerURLContext;
	}

	public void setEurekaServerPort(String eurekaServerPort) {
		this.eurekaServerPort = eurekaServerPort;
	}

	public void setEurekaServerDNSName(String eurekaServerDNSName) {
		this.eurekaServerDNSName = eurekaServerDNSName;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public void setEurekaConnectionIdleTimeoutSeconds(
			int eurekaConnectionIdleTimeoutSeconds) {
		this.eurekaConnectionIdleTimeoutSeconds = eurekaConnectionIdleTimeoutSeconds;
	}

	public void setRegistryRefreshSingleVipAddress(
			String registryRefreshSingleVipAddress) {
		this.registryRefreshSingleVipAddress = registryRefreshSingleVipAddress;
	}

	public void setHeartbeatExecutorThreadPoolSize(int heartbeatExecutorThreadPoolSize) {
		this.heartbeatExecutorThreadPoolSize = heartbeatExecutorThreadPoolSize;
	}

	public void setHeartbeatExecutorExponentialBackOffBound(
			int heartbeatExecutorExponentialBackOffBound) {
		this.heartbeatExecutorExponentialBackOffBound = heartbeatExecutorExponentialBackOffBound;
	}

	public void setCacheRefreshExecutorThreadPoolSize(
			int cacheRefreshExecutorThreadPoolSize) {
		this.cacheRefreshExecutorThreadPoolSize = cacheRefreshExecutorThreadPoolSize;
	}

	public void setCacheRefreshExecutorExponentialBackOffBound(
			int cacheRefreshExecutorExponentialBackOffBound) {
		this.cacheRefreshExecutorExponentialBackOffBound = cacheRefreshExecutorExponentialBackOffBound;
	}

	public void setServiceUrl(Map<String, String> serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

	public void setGZipContent(boolean gZipContent) {
		this.gZipContent = gZipContent;
	}

	public void setUseDnsForFetchingServiceUrls(boolean useDnsForFetchingServiceUrls) {
		this.useDnsForFetchingServiceUrls = useDnsForFetchingServiceUrls;
	}

	public void setRegisterWithEureka(boolean registerWithEureka) {
		this.registerWithEureka = registerWithEureka;
	}

	public void setPreferSameZoneEureka(boolean preferSameZoneEureka) {
		this.preferSameZoneEureka = preferSameZoneEureka;
	}

	public void setLogDeltaDiff(boolean logDeltaDiff) {
		this.logDeltaDiff = logDeltaDiff;
	}

	public void setDisableDelta(boolean disableDelta) {
		this.disableDelta = disableDelta;
	}

	public void setFetchRemoteRegionsRegistry(String fetchRemoteRegionsRegistry) {
		this.fetchRemoteRegionsRegistry = fetchRemoteRegionsRegistry;
	}

	public void setAvailabilityZones(Map<String, String> availabilityZones) {
		this.availabilityZones = availabilityZones;
	}

	public void setFilterOnlyUpInstances(boolean filterOnlyUpInstances) {
		this.filterOnlyUpInstances = filterOnlyUpInstances;
	}

	public void setFetchRegistry(boolean fetchRegistry) {
		this.fetchRegistry = fetchRegistry;
	}

	public void setDollarReplacement(String dollarReplacement) {
		this.dollarReplacement = dollarReplacement;
	}

	public void setEscapeCharReplacement(String escapeCharReplacement) {
		this.escapeCharReplacement = escapeCharReplacement;
	}

	public void setAllowRedirects(boolean allowRedirects) {
		this.allowRedirects = allowRedirects;
	}

	public void setOnDemandUpdateStatusChange(boolean onDemandUpdateStatusChange) {
		this.onDemandUpdateStatusChange = onDemandUpdateStatusChange;
	}

	public void setEncoderName(String encoderName) {
		this.encoderName = encoderName;
	}

	public void setDecoderName(String decoderName) {
		this.decoderName = decoderName;
	}

	public void setClientDataAccept(String clientDataAccept) {
		this.clientDataAccept = clientDataAccept;
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof EurekaClientConfigBean))
			return false;
		final EurekaClientConfigBean other = (EurekaClientConfigBean) o;
		if (!other.canEqual((Object) this))
			return false;
		final Object this$propertyResolver = this.propertyResolver;
		final Object other$propertyResolver = other.propertyResolver;
		if (this$propertyResolver == null ?
				other$propertyResolver != null :
				!this$propertyResolver.equals(other$propertyResolver))
			return false;
		if (this.enabled != other.enabled)
			return false;
		final Object this$transport = this.transport;
		final Object other$transport = other.transport;
		if (this$transport == null ?
				other$transport != null :
				!this$transport.equals(other$transport))
			return false;
		if (this.registryFetchIntervalSeconds != other.registryFetchIntervalSeconds)
			return false;
		if (this.instanceInfoReplicationIntervalSeconds
				!= other.instanceInfoReplicationIntervalSeconds)
			return false;
		if (this.initialInstanceInfoReplicationIntervalSeconds
				!= other.initialInstanceInfoReplicationIntervalSeconds)
			return false;
		if (this.eurekaServiceUrlPollIntervalSeconds
				!= other.eurekaServiceUrlPollIntervalSeconds)
			return false;
		final Object this$proxyPort = this.proxyPort;
		final Object other$proxyPort = other.proxyPort;
		if (this$proxyPort == null ?
				other$proxyPort != null :
				!this$proxyPort.equals(other$proxyPort))
			return false;
		final Object this$proxyHost = this.proxyHost;
		final Object other$proxyHost = other.proxyHost;
		if (this$proxyHost == null ?
				other$proxyHost != null :
				!this$proxyHost.equals(other$proxyHost))
			return false;
		final Object this$proxyUserName = this.proxyUserName;
		final Object other$proxyUserName = other.proxyUserName;
		if (this$proxyUserName == null ?
				other$proxyUserName != null :
				!this$proxyUserName.equals(other$proxyUserName))
			return false;
		final Object this$proxyPassword = this.proxyPassword;
		final Object other$proxyPassword = other.proxyPassword;
		if (this$proxyPassword == null ?
				other$proxyPassword != null :
				!this$proxyPassword.equals(other$proxyPassword))
			return false;
		if (this.eurekaServerReadTimeoutSeconds != other.eurekaServerReadTimeoutSeconds)
			return false;
		if (this.eurekaServerConnectTimeoutSeconds
				!= other.eurekaServerConnectTimeoutSeconds)
			return false;
		final Object this$backupRegistryImpl = this.backupRegistryImpl;
		final Object other$backupRegistryImpl = other.backupRegistryImpl;
		if (this$backupRegistryImpl == null ?
				other$backupRegistryImpl != null :
				!this$backupRegistryImpl.equals(other$backupRegistryImpl))
			return false;
		if (this.eurekaServerTotalConnections != other.eurekaServerTotalConnections)
			return false;
		if (this.eurekaServerTotalConnectionsPerHost
				!= other.eurekaServerTotalConnectionsPerHost)
			return false;
		final Object this$eurekaServerURLContext = this.eurekaServerURLContext;
		final Object other$eurekaServerURLContext = other.eurekaServerURLContext;
		if (this$eurekaServerURLContext == null ?
				other$eurekaServerURLContext != null :
				!this$eurekaServerURLContext.equals(other$eurekaServerURLContext))
			return false;
		final Object this$eurekaServerPort = this.eurekaServerPort;
		final Object other$eurekaServerPort = other.eurekaServerPort;
		if (this$eurekaServerPort == null ?
				other$eurekaServerPort != null :
				!this$eurekaServerPort.equals(other$eurekaServerPort))
			return false;
		final Object this$eurekaServerDNSName = this.eurekaServerDNSName;
		final Object other$eurekaServerDNSName = other.eurekaServerDNSName;
		if (this$eurekaServerDNSName == null ?
				other$eurekaServerDNSName != null :
				!this$eurekaServerDNSName.equals(other$eurekaServerDNSName))
			return false;
		final Object this$region = this.region;
		final Object other$region = other.region;
		if (this$region == null ?
				other$region != null :
				!this$region.equals(other$region))
			return false;
		if (this.eurekaConnectionIdleTimeoutSeconds
				!= other.eurekaConnectionIdleTimeoutSeconds)
			return false;
		final Object this$registryRefreshSingleVipAddress = this.registryRefreshSingleVipAddress;
		final Object other$registryRefreshSingleVipAddress = other.registryRefreshSingleVipAddress;
		if (this$registryRefreshSingleVipAddress == null ?
				other$registryRefreshSingleVipAddress != null :
				!this$registryRefreshSingleVipAddress
						.equals(other$registryRefreshSingleVipAddress))
			return false;
		if (this.heartbeatExecutorThreadPoolSize != other.heartbeatExecutorThreadPoolSize)
			return false;
		if (this.heartbeatExecutorExponentialBackOffBound
				!= other.heartbeatExecutorExponentialBackOffBound)
			return false;
		if (this.cacheRefreshExecutorThreadPoolSize
				!= other.cacheRefreshExecutorThreadPoolSize)
			return false;
		if (this.cacheRefreshExecutorExponentialBackOffBound
				!= other.cacheRefreshExecutorExponentialBackOffBound)
			return false;
		final Object this$serviceUrl = this.serviceUrl;
		final Object other$serviceUrl = other.serviceUrl;
		if (this$serviceUrl == null ?
				other$serviceUrl != null :
				!this$serviceUrl.equals(other$serviceUrl))
			return false;
		if (this.gZipContent != other.gZipContent)
			return false;
		if (this.useDnsForFetchingServiceUrls != other.useDnsForFetchingServiceUrls)
			return false;
		if (this.registerWithEureka != other.registerWithEureka)
			return false;
		if (this.preferSameZoneEureka != other.preferSameZoneEureka)
			return false;
		if (this.logDeltaDiff != other.logDeltaDiff)
			return false;
		if (this.disableDelta != other.disableDelta)
			return false;
		final Object this$fetchRemoteRegionsRegistry = this.fetchRemoteRegionsRegistry;
		final Object other$fetchRemoteRegionsRegistry = other.fetchRemoteRegionsRegistry;
		if (this$fetchRemoteRegionsRegistry == null ?
				other$fetchRemoteRegionsRegistry != null :
				!this$fetchRemoteRegionsRegistry.equals(other$fetchRemoteRegionsRegistry))
			return false;
		final Object this$availabilityZones = this.getAvailabilityZones();
		final Object other$availabilityZones = other.getAvailabilityZones();
		if (this$availabilityZones == null ?
				other$availabilityZones != null :
				!this$availabilityZones.equals(other$availabilityZones))
			return false;
		if (this.filterOnlyUpInstances != other.filterOnlyUpInstances)
			return false;
		if (this.fetchRegistry != other.fetchRegistry)
			return false;
		final Object this$dollarReplacement = this.dollarReplacement;
		final Object other$dollarReplacement = other.dollarReplacement;
		if (this$dollarReplacement == null ?
				other$dollarReplacement != null :
				!this$dollarReplacement.equals(other$dollarReplacement))
			return false;
		final Object this$escapeCharReplacement = this.escapeCharReplacement;
		final Object other$escapeCharReplacement = other.escapeCharReplacement;
		if (this$escapeCharReplacement == null ?
				other$escapeCharReplacement != null :
				!this$escapeCharReplacement.equals(other$escapeCharReplacement))
			return false;
		if (this.allowRedirects != other.allowRedirects)
			return false;
		if (this.onDemandUpdateStatusChange != other.onDemandUpdateStatusChange)
			return false;
		final Object this$encoderName = this.encoderName;
		final Object other$encoderName = other.encoderName;
		if (this$encoderName == null ?
				other$encoderName != null :
				!this$encoderName.equals(other$encoderName))
			return false;
		final Object this$decoderName = this.decoderName;
		final Object other$decoderName = other.decoderName;
		if (this$decoderName == null ?
				other$decoderName != null :
				!this$decoderName.equals(other$decoderName))
			return false;
		final Object this$clientDataAccept = this.clientDataAccept;
		final Object other$clientDataAccept = other.clientDataAccept;
		if (this$clientDataAccept == null ?
				other$clientDataAccept != null :
				!this$clientDataAccept.equals(other$clientDataAccept))
			return false;
		return true;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $propertyResolver = this.propertyResolver;
		result = result * PRIME + ($propertyResolver == null ?
				0 :
				$propertyResolver.hashCode());
		result = result * PRIME + (this.enabled ? 79 : 97);
		final Object $transport = this.transport;
		result = result * PRIME + ($transport == null ? 0 : $transport.hashCode());
		result = result * PRIME + this.registryFetchIntervalSeconds;
		result = result * PRIME + this.instanceInfoReplicationIntervalSeconds;
		result = result * PRIME + this.initialInstanceInfoReplicationIntervalSeconds;
		result = result * PRIME + this.eurekaServiceUrlPollIntervalSeconds;
		final Object $proxyPort = this.proxyPort;
		result = result * PRIME + ($proxyPort == null ? 0 : $proxyPort.hashCode());
		final Object $proxyHost = this.proxyHost;
		result = result * PRIME + ($proxyHost == null ? 0 : $proxyHost.hashCode());
		final Object $proxyUserName = this.proxyUserName;
		result =
				result * PRIME + ($proxyUserName == null ? 0 : $proxyUserName.hashCode());
		final Object $proxyPassword = this.proxyPassword;
		result =
				result * PRIME + ($proxyPassword == null ? 0 : $proxyPassword.hashCode());
		result = result * PRIME + this.eurekaServerReadTimeoutSeconds;
		result = result * PRIME + this.eurekaServerConnectTimeoutSeconds;
		final Object $backupRegistryImpl = this.backupRegistryImpl;
		result = result * PRIME + ($backupRegistryImpl == null ?
				0 :
				$backupRegistryImpl.hashCode());
		result = result * PRIME + this.eurekaServerTotalConnections;
		result = result * PRIME + this.eurekaServerTotalConnectionsPerHost;
		final Object $eurekaServerURLContext = this.eurekaServerURLContext;
		result = result * PRIME + ($eurekaServerURLContext == null ?
				0 :
				$eurekaServerURLContext.hashCode());
		final Object $eurekaServerPort = this.eurekaServerPort;
		result = result * PRIME + ($eurekaServerPort == null ?
				0 :
				$eurekaServerPort.hashCode());
		final Object $eurekaServerDNSName = this.eurekaServerDNSName;
		result = result * PRIME + ($eurekaServerDNSName == null ?
				0 :
				$eurekaServerDNSName.hashCode());
		final Object $region = this.region;
		result = result * PRIME + ($region == null ? 0 : $region.hashCode());
		result = result * PRIME + this.eurekaConnectionIdleTimeoutSeconds;
		final Object $registryRefreshSingleVipAddress = this.registryRefreshSingleVipAddress;
		result = result * PRIME + ($registryRefreshSingleVipAddress == null ?
				0 :
				$registryRefreshSingleVipAddress.hashCode());
		result = result * PRIME + this.heartbeatExecutorThreadPoolSize;
		result = result * PRIME + this.heartbeatExecutorExponentialBackOffBound;
		result = result * PRIME + this.cacheRefreshExecutorThreadPoolSize;
		result = result * PRIME + this.cacheRefreshExecutorExponentialBackOffBound;
		final Object $serviceUrl = this.serviceUrl;
		result = result * PRIME + ($serviceUrl == null ? 0 : $serviceUrl.hashCode());
		result = result * PRIME + (this.gZipContent ? 79 : 97);
		result = result * PRIME + (this.useDnsForFetchingServiceUrls ? 79 : 97);
		result = result * PRIME + (this.registerWithEureka ? 79 : 97);
		result = result * PRIME + (this.preferSameZoneEureka ? 79 : 97);
		result = result * PRIME + (this.logDeltaDiff ? 79 : 97);
		result = result * PRIME + (this.disableDelta ? 79 : 97);
		final Object $fetchRemoteRegionsRegistry = this.fetchRemoteRegionsRegistry;
		result = result * PRIME + ($fetchRemoteRegionsRegistry == null ?
				0 :
				$fetchRemoteRegionsRegistry.hashCode());
		final Object $availabilityZones = this.getAvailabilityZones();
		result = result * PRIME + ($availabilityZones == null ?
				0 :
				$availabilityZones.hashCode());
		result = result * PRIME + (this.filterOnlyUpInstances ? 79 : 97);
		result = result * PRIME + (this.fetchRegistry ? 79 : 97);
		final Object $dollarReplacement = this.dollarReplacement;
		result = result * PRIME + ($dollarReplacement == null ?
				0 :
				$dollarReplacement.hashCode());
		final Object $escapeCharReplacement = this.escapeCharReplacement;
		result = result * PRIME + ($escapeCharReplacement == null ?
				0 :
				$escapeCharReplacement.hashCode());
		result = result * PRIME + (this.allowRedirects ? 79 : 97);
		result = result * PRIME + (this.onDemandUpdateStatusChange ? 79 : 97);
		final Object $encoderName = this.encoderName;
		result = result * PRIME + ($encoderName == null ? 0 : $encoderName.hashCode());
		final Object $decoderName = this.decoderName;
		result = result * PRIME + ($decoderName == null ? 0 : $decoderName.hashCode());
		final Object $clientDataAccept = this.clientDataAccept;
		result = result * PRIME + ($clientDataAccept == null ?
				0 :
				$clientDataAccept.hashCode());
		return result;
	}

	protected boolean canEqual(Object other) {
		return other instanceof EurekaClientConfigBean;
	}

	public String toString() {
		return "org.springframework.cloud.netflix.eureka.EurekaClientConfigBean(propertyResolver="
				+ this.propertyResolver + ", enabled=" + this.enabled + ", transport="
				+ this.transport + ", registryFetchIntervalSeconds="
				+ this.registryFetchIntervalSeconds
				+ ", instanceInfoReplicationIntervalSeconds="
				+ this.instanceInfoReplicationIntervalSeconds
				+ ", initialInstanceInfoReplicationIntervalSeconds="
				+ this.initialInstanceInfoReplicationIntervalSeconds
				+ ", eurekaServiceUrlPollIntervalSeconds="
				+ this.eurekaServiceUrlPollIntervalSeconds + ", proxyPort="
				+ this.proxyPort + ", proxyHost=" + this.proxyHost + ", proxyUserName="
				+ this.proxyUserName + ", proxyPassword=" + this.proxyPassword
				+ ", eurekaServerReadTimeoutSeconds="
				+ this.eurekaServerReadTimeoutSeconds
				+ ", eurekaServerConnectTimeoutSeconds="
				+ this.eurekaServerConnectTimeoutSeconds + ", backupRegistryImpl="
				+ this.backupRegistryImpl + ", eurekaServerTotalConnections="
				+ this.eurekaServerTotalConnections
				+ ", eurekaServerTotalConnectionsPerHost="
				+ this.eurekaServerTotalConnectionsPerHost + ", eurekaServerURLContext="
				+ this.eurekaServerURLContext + ", eurekaServerPort="
				+ this.eurekaServerPort + ", eurekaServerDNSName="
				+ this.eurekaServerDNSName + ", region=" + this.region
				+ ", eurekaConnectionIdleTimeoutSeconds="
				+ this.eurekaConnectionIdleTimeoutSeconds
				+ ", registryRefreshSingleVipAddress="
				+ this.registryRefreshSingleVipAddress
				+ ", heartbeatExecutorThreadPoolSize="
				+ this.heartbeatExecutorThreadPoolSize
				+ ", heartbeatExecutorExponentialBackOffBound="
				+ this.heartbeatExecutorExponentialBackOffBound
				+ ", cacheRefreshExecutorThreadPoolSize="
				+ this.cacheRefreshExecutorThreadPoolSize
				+ ", cacheRefreshExecutorExponentialBackOffBound="
				+ this.cacheRefreshExecutorExponentialBackOffBound + ", serviceUrl="
				+ this.serviceUrl + ", gZipContent=" + this.gZipContent
				+ ", useDnsForFetchingServiceUrls=" + this.useDnsForFetchingServiceUrls
				+ ", registerWithEureka=" + this.registerWithEureka
				+ ", preferSameZoneEureka=" + this.preferSameZoneEureka
				+ ", logDeltaDiff=" + this.logDeltaDiff + ", disableDelta="
				+ this.disableDelta + ", fetchRemoteRegionsRegistry="
				+ this.fetchRemoteRegionsRegistry + ", availabilityZones=" + this
				.getAvailabilityZones() + ", filterOnlyUpInstances="
				+ this.filterOnlyUpInstances + ", fetchRegistry=" + this.fetchRegistry
				+ ", dollarReplacement=" + this.dollarReplacement
				+ ", escapeCharReplacement=" + this.escapeCharReplacement
				+ ", allowRedirects=" + this.allowRedirects
				+ ", onDemandUpdateStatusChange=" + this.onDemandUpdateStatusChange
				+ ", encoderName=" + this.encoderName + ", decoderName="
				+ this.decoderName + ", clientDataAccept=" + this.clientDataAccept + ")";
	}
}
