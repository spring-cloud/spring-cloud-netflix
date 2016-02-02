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

package org.springframework.cloud.netflix.eureka.server;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.aws.AwsBindingStrategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.netflix.eureka.EurekaConstants;
import org.springframework.core.env.PropertyResolver;

/**
 * @author Dave Syer
 */
@ConfigurationProperties(EurekaServerConfigBean.PREFIX)
public class EurekaServerConfigBean implements EurekaServerConfig, EurekaConstants {

	public static final String PREFIX = "eureka.server";

	private static final int MINUTES = 60 * 1000;

	@Autowired(required = false)
	PropertyResolver propertyResolver;

	private String aWSAccessId;

	private String aWSSecretKey;

	private int eIPBindRebindRetries = 3;

	private int eIPBindingRetryIntervalMs = 5 * MINUTES;

	private int eIPBindingRetryIntervalMsWhenUnbound = 1 * MINUTES;

	private boolean enableSelfPreservation = true;

	private double renewalPercentThreshold = 0.85;

	private int renewalThresholdUpdateIntervalMs = 15 * MINUTES;

	private int peerEurekaNodesUpdateIntervalMs = 10 * MINUTES;

	private int numberOfReplicationRetries = 5;

	private int peerEurekaStatusRefreshTimeIntervalMs = 30 * 1000;

	private int waitTimeInMsWhenSyncEmpty = 5 * MINUTES;

	private int peerNodeConnectTimeoutMs = 200;

	private int peerNodeReadTimeoutMs = 200;

	private int peerNodeTotalConnections = 1000;

	private int peerNodeTotalConnectionsPerHost = 500;

	private int peerNodeConnectionIdleTimeoutSeconds = 30;

	private long retentionTimeInMSInDeltaQueue = 3 * MINUTES;

	private long deltaRetentionTimerIntervalInMs = 30 * 1000;

	private long evictionIntervalTimerInMs = 60 * 1000;

	private int aSGQueryTimeoutMs = 300;

	private long aSGUpdateIntervalMs = 5 * MINUTES;

	private long aSGCacheExpiryTimeoutMs = 10 * MINUTES; // defaults to longer than the asg update interval

	private long responseCacheAutoExpirationInSeconds = 180;

	private long responseCacheUpdateIntervalMs = 30 * 1000;

	private boolean useReadOnlyResponseCache = true;

	private boolean disableDelta;

	private long maxIdleThreadInMinutesAgeForStatusReplication = 10;

	private int minThreadsForStatusReplication = 1;

	private int maxThreadsForStatusReplication = 1;

	private int maxElementsInStatusReplicationPool = 10000;

	private boolean syncWhenTimestampDiffers = true;

	//TODO: what should these defaults be? for single first?
	private int registrySyncRetries = 0;

	private long registrySyncRetryWaitMs = 30 * 1000;

	private int maxElementsInPeerReplicationPool = 10000;

	private long maxIdleThreadAgeInMinutesForPeerReplication = 15;

	private int minThreadsForPeerReplication = 5;

	private int maxThreadsForPeerReplication = 20;

	private int maxTimeForReplication = 30000;

	private boolean primeAwsReplicaConnections = true;

	private boolean disableDeltaForRemoteRegions;

	private int remoteRegionConnectTimeoutMs = 1000;

	private int remoteRegionReadTimeoutMs = 1000;

	private int remoteRegionTotalConnections = 1000;

	private int remoteRegionTotalConnectionsPerHost = 500;

	private int remoteRegionConnectionIdleTimeoutSeconds = 30;

	private boolean gZipContentFromRemoteRegion = true;

	private Map<String, String> remoteRegionUrlsWithName = new HashMap<>();

	private String[] remoteRegionUrls;

	private Map<String, Set<String>> remoteRegionAppWhitelist;

	private int remoteRegionRegistryFetchInterval = 30;

	private int remoteRegionFetchThreadPoolSize = 20;

	private String remoteRegionTrustStore = "";

	private String remoteRegionTrustStorePassword = "changeit";

	private boolean disableTransparentFallbackToOtherRegion;

	private boolean batchReplication;

	private boolean rateLimiterEnabled = false;

	private boolean rateLimiterThrottleStandardClients = false;

	private Set<String> rateLimiterPrivilegedClients = Collections.emptySet();

	private int rateLimiterBurstSize = 10;

	private int rateLimiterRegistryFetchAverageRate = 500;

	private int rateLimiterFullFetchAverageRate = 100;

	private boolean logIdentityHeaders = true;

	private String listAutoScalingGroupsRoleName = "ListAutoScalingGroups";

	private boolean enableReplicatedRequestCompression = false;

	private String jsonCodecName;

	private String xmlCodecName;

	private int route53BindRebindRetries = 3;

	private int route53BindingRetryIntervalMs = 5 * MINUTES;

	private long route53DomainTTL = 30;

	private AwsBindingStrategy bindingStrategy = AwsBindingStrategy.EIP;

	public EurekaServerConfigBean() {
	}

	@Override
	public boolean shouldEnableSelfPreservation() {
		return this.enableSelfPreservation;
	}

	@Override
	public boolean shouldDisableDelta() {
		return this.disableDelta;
	}

	@Override
	public boolean shouldSyncWhenTimestampDiffers() {
		return this.syncWhenTimestampDiffers;
	}

	@Override
	public boolean shouldPrimeAwsReplicaConnections() {
		return this.primeAwsReplicaConnections;
	}

	@Override
	public boolean shouldDisableDeltaForRemoteRegions() {
		return this.disableDeltaForRemoteRegions;
	}

	@Override
	public boolean shouldGZipContentFromRemoteRegion() {
		return this.gZipContentFromRemoteRegion;
	}

	@Override
	public Set<String> getRemoteRegionAppWhitelist(String regionName) {
		return this.remoteRegionAppWhitelist.get(regionName == null ? "global"
				: regionName.trim().toLowerCase());
	}

	@Override
	public boolean disableTransparentFallbackToOtherRegion() {
		return this.disableTransparentFallbackToOtherRegion;
	}

	@Override
	public boolean shouldBatchReplication() {
		return this.batchReplication;
	}

	@Override
	public boolean shouldLogIdentityHeaders() {
		return this.logIdentityHeaders;
	}

	@Override
	public String getJsonCodecName() {
		return jsonCodecName;
	}

	@Override
	public String getXmlCodecName() {
		return xmlCodecName;
	}

	@Override
	public boolean shouldUseReadOnlyResponseCache() {
		return this.useReadOnlyResponseCache;
	}

	@Override
	public boolean shouldEnableReplicatedRequestCompression() {
		return this.enableReplicatedRequestCompression;
	}

	@Override
	public String getExperimental(String name) {
		if (propertyResolver != null) {
			return propertyResolver.getProperty(PREFIX + ".experimental." + name,
					String.class, null);
		}
		return null;
	}

	public PropertyResolver getPropertyResolver() {
		return this.propertyResolver;
	}

	public String getAWSAccessId() {
		return this.aWSAccessId;
	}

	public String getAWSSecretKey() {
		return this.aWSSecretKey;
	}

	public int getEIPBindRebindRetries() {
		return this.eIPBindRebindRetries;
	}

	public int getEIPBindingRetryIntervalMs() {
		return this.eIPBindingRetryIntervalMs;
	}

	public int getEIPBindingRetryIntervalMsWhenUnbound() {
		return this.eIPBindingRetryIntervalMsWhenUnbound;
	}

	public boolean isEnableSelfPreservation() {
		return this.enableSelfPreservation;
	}

	public double getRenewalPercentThreshold() {
		return this.renewalPercentThreshold;
	}

	public int getRenewalThresholdUpdateIntervalMs() {
		return this.renewalThresholdUpdateIntervalMs;
	}

	public int getPeerEurekaNodesUpdateIntervalMs() {
		return this.peerEurekaNodesUpdateIntervalMs;
	}

	public int getNumberOfReplicationRetries() {
		return this.numberOfReplicationRetries;
	}

	public int getPeerEurekaStatusRefreshTimeIntervalMs() {
		return this.peerEurekaStatusRefreshTimeIntervalMs;
	}

	public int getWaitTimeInMsWhenSyncEmpty() {
		return this.waitTimeInMsWhenSyncEmpty;
	}

	public int getPeerNodeConnectTimeoutMs() {
		return this.peerNodeConnectTimeoutMs;
	}

	public int getPeerNodeReadTimeoutMs() {
		return this.peerNodeReadTimeoutMs;
	}

	public int getPeerNodeTotalConnections() {
		return this.peerNodeTotalConnections;
	}

	public int getPeerNodeTotalConnectionsPerHost() {
		return this.peerNodeTotalConnectionsPerHost;
	}

	public int getPeerNodeConnectionIdleTimeoutSeconds() {
		return this.peerNodeConnectionIdleTimeoutSeconds;
	}

	public long getRetentionTimeInMSInDeltaQueue() {
		return this.retentionTimeInMSInDeltaQueue;
	}

	public long getDeltaRetentionTimerIntervalInMs() {
		return this.deltaRetentionTimerIntervalInMs;
	}

	public long getEvictionIntervalTimerInMs() {
		return this.evictionIntervalTimerInMs;
	}

	public int getASGQueryTimeoutMs() {
		return this.aSGQueryTimeoutMs;
	}

	public long getASGUpdateIntervalMs() {
		return this.aSGUpdateIntervalMs;
	}

	public long getASGCacheExpiryTimeoutMs() {
		return this.aSGCacheExpiryTimeoutMs;
	}

	public long getResponseCacheAutoExpirationInSeconds() {
		return this.responseCacheAutoExpirationInSeconds;
	}

	public long getResponseCacheUpdateIntervalMs() {
		return this.responseCacheUpdateIntervalMs;
	}

	public boolean isUseReadOnlyResponseCache() {
		return this.useReadOnlyResponseCache;
	}

	public boolean isDisableDelta() {
		return this.disableDelta;
	}

	public long getMaxIdleThreadInMinutesAgeForStatusReplication() {
		return this.maxIdleThreadInMinutesAgeForStatusReplication;
	}

	public int getMinThreadsForStatusReplication() {
		return this.minThreadsForStatusReplication;
	}

	public int getMaxThreadsForStatusReplication() {
		return this.maxThreadsForStatusReplication;
	}

	public int getMaxElementsInStatusReplicationPool() {
		return this.maxElementsInStatusReplicationPool;
	}

	public boolean isSyncWhenTimestampDiffers() {
		return this.syncWhenTimestampDiffers;
	}

	public int getRegistrySyncRetries() {
		return this.registrySyncRetries;
	}

	public long getRegistrySyncRetryWaitMs() {
		return this.registrySyncRetryWaitMs;
	}

	public int getMaxElementsInPeerReplicationPool() {
		return this.maxElementsInPeerReplicationPool;
	}

	public long getMaxIdleThreadAgeInMinutesForPeerReplication() {
		return this.maxIdleThreadAgeInMinutesForPeerReplication;
	}

	public int getMinThreadsForPeerReplication() {
		return this.minThreadsForPeerReplication;
	}

	public int getMaxThreadsForPeerReplication() {
		return this.maxThreadsForPeerReplication;
	}

	public int getMaxTimeForReplication() {
		return this.maxTimeForReplication;
	}

	public boolean isPrimeAwsReplicaConnections() {
		return this.primeAwsReplicaConnections;
	}

	public boolean isDisableDeltaForRemoteRegions() {
		return this.disableDeltaForRemoteRegions;
	}

	public int getRemoteRegionConnectTimeoutMs() {
		return this.remoteRegionConnectTimeoutMs;
	}

	public int getRemoteRegionReadTimeoutMs() {
		return this.remoteRegionReadTimeoutMs;
	}

	public int getRemoteRegionTotalConnections() {
		return this.remoteRegionTotalConnections;
	}

	public int getRemoteRegionTotalConnectionsPerHost() {
		return this.remoteRegionTotalConnectionsPerHost;
	}

	public int getRemoteRegionConnectionIdleTimeoutSeconds() {
		return this.remoteRegionConnectionIdleTimeoutSeconds;
	}

	public boolean isGZipContentFromRemoteRegion() {
		return this.gZipContentFromRemoteRegion;
	}

	public Map<String, String> getRemoteRegionUrlsWithName() {
		return this.remoteRegionUrlsWithName;
	}

	public String[] getRemoteRegionUrls() {
		return this.remoteRegionUrls;
	}

	public Map<String, Set<String>> getRemoteRegionAppWhitelist() {
		return this.remoteRegionAppWhitelist;
	}

	public int getRemoteRegionRegistryFetchInterval() {
		return this.remoteRegionRegistryFetchInterval;
	}

	public int getRemoteRegionFetchThreadPoolSize() {
		return this.remoteRegionFetchThreadPoolSize;
	}

	public String getRemoteRegionTrustStore() {
		return this.remoteRegionTrustStore;
	}

	public String getRemoteRegionTrustStorePassword() {
		return this.remoteRegionTrustStorePassword;
	}

	public boolean isDisableTransparentFallbackToOtherRegion() {
		return this.disableTransparentFallbackToOtherRegion;
	}

	public boolean isBatchReplication() {
		return this.batchReplication;
	}

	public boolean isRateLimiterEnabled() {
		return this.rateLimiterEnabled;
	}

	public boolean isRateLimiterThrottleStandardClients() {
		return this.rateLimiterThrottleStandardClients;
	}

	public Set<String> getRateLimiterPrivilegedClients() {
		return this.rateLimiterPrivilegedClients;
	}

	public int getRateLimiterBurstSize() {
		return this.rateLimiterBurstSize;
	}

	public int getRateLimiterRegistryFetchAverageRate() {
		return this.rateLimiterRegistryFetchAverageRate;
	}

	public int getRateLimiterFullFetchAverageRate() {
		return this.rateLimiterFullFetchAverageRate;
	}

	public boolean isLogIdentityHeaders() {
		return this.logIdentityHeaders;
	}

	public String getListAutoScalingGroupsRoleName() {
		return this.listAutoScalingGroupsRoleName;
	}

	public boolean isEnableReplicatedRequestCompression() {
		return this.enableReplicatedRequestCompression;
	}

	public int getRoute53BindRebindRetries() {
		return this.route53BindRebindRetries;
	}

	public int getRoute53BindingRetryIntervalMs() {
		return this.route53BindingRetryIntervalMs;
	}

	public long getRoute53DomainTTL() {
		return this.route53DomainTTL;
	}

	public AwsBindingStrategy getBindingStrategy() {
		return this.bindingStrategy;
	}

	public void setPropertyResolver(PropertyResolver propertyResolver) {
		this.propertyResolver = propertyResolver;
	}

	public void setAWSAccessId(String aWSAccessId) {
		this.aWSAccessId = aWSAccessId;
	}

	public void setAWSSecretKey(String aWSSecretKey) {
		this.aWSSecretKey = aWSSecretKey;
	}

	public void setEIPBindRebindRetries(int eIPBindRebindRetries) {
		this.eIPBindRebindRetries = eIPBindRebindRetries;
	}

	public void setEIPBindingRetryIntervalMs(int eIPBindingRetryIntervalMs) {
		this.eIPBindingRetryIntervalMs = eIPBindingRetryIntervalMs;
	}

	public void setEIPBindingRetryIntervalMsWhenUnbound(
			int eIPBindingRetryIntervalMsWhenUnbound) {
		this.eIPBindingRetryIntervalMsWhenUnbound = eIPBindingRetryIntervalMsWhenUnbound;
	}

	public void setEnableSelfPreservation(boolean enableSelfPreservation) {
		this.enableSelfPreservation = enableSelfPreservation;
	}

	public void setRenewalPercentThreshold(double renewalPercentThreshold) {
		this.renewalPercentThreshold = renewalPercentThreshold;
	}

	public void setRenewalThresholdUpdateIntervalMs(
			int renewalThresholdUpdateIntervalMs) {
		this.renewalThresholdUpdateIntervalMs = renewalThresholdUpdateIntervalMs;
	}

	public void setPeerEurekaNodesUpdateIntervalMs(int peerEurekaNodesUpdateIntervalMs) {
		this.peerEurekaNodesUpdateIntervalMs = peerEurekaNodesUpdateIntervalMs;
	}

	public void setNumberOfReplicationRetries(int numberOfReplicationRetries) {
		this.numberOfReplicationRetries = numberOfReplicationRetries;
	}

	public void setPeerEurekaStatusRefreshTimeIntervalMs(
			int peerEurekaStatusRefreshTimeIntervalMs) {
		this.peerEurekaStatusRefreshTimeIntervalMs = peerEurekaStatusRefreshTimeIntervalMs;
	}

	public void setWaitTimeInMsWhenSyncEmpty(int waitTimeInMsWhenSyncEmpty) {
		this.waitTimeInMsWhenSyncEmpty = waitTimeInMsWhenSyncEmpty;
	}

	public void setPeerNodeConnectTimeoutMs(int peerNodeConnectTimeoutMs) {
		this.peerNodeConnectTimeoutMs = peerNodeConnectTimeoutMs;
	}

	public void setPeerNodeReadTimeoutMs(int peerNodeReadTimeoutMs) {
		this.peerNodeReadTimeoutMs = peerNodeReadTimeoutMs;
	}

	public void setPeerNodeTotalConnections(int peerNodeTotalConnections) {
		this.peerNodeTotalConnections = peerNodeTotalConnections;
	}

	public void setPeerNodeTotalConnectionsPerHost(int peerNodeTotalConnectionsPerHost) {
		this.peerNodeTotalConnectionsPerHost = peerNodeTotalConnectionsPerHost;
	}

	public void setPeerNodeConnectionIdleTimeoutSeconds(
			int peerNodeConnectionIdleTimeoutSeconds) {
		this.peerNodeConnectionIdleTimeoutSeconds = peerNodeConnectionIdleTimeoutSeconds;
	}

	public void setRetentionTimeInMSInDeltaQueue(long retentionTimeInMSInDeltaQueue) {
		this.retentionTimeInMSInDeltaQueue = retentionTimeInMSInDeltaQueue;
	}

	public void setDeltaRetentionTimerIntervalInMs(long deltaRetentionTimerIntervalInMs) {
		this.deltaRetentionTimerIntervalInMs = deltaRetentionTimerIntervalInMs;
	}

	public void setEvictionIntervalTimerInMs(long evictionIntervalTimerInMs) {
		this.evictionIntervalTimerInMs = evictionIntervalTimerInMs;
	}

	public void setASGQueryTimeoutMs(int aSGQueryTimeoutMs) {
		this.aSGQueryTimeoutMs = aSGQueryTimeoutMs;
	}

	public void setASGUpdateIntervalMs(long aSGUpdateIntervalMs) {
		this.aSGUpdateIntervalMs = aSGUpdateIntervalMs;
	}

	public void setASGCacheExpiryTimeoutMs(long aSGCacheExpiryTimeoutMs) {
		this.aSGCacheExpiryTimeoutMs = aSGCacheExpiryTimeoutMs;
	}

	public void setResponseCacheAutoExpirationInSeconds(
			long responseCacheAutoExpirationInSeconds) {
		this.responseCacheAutoExpirationInSeconds = responseCacheAutoExpirationInSeconds;
	}

	public void setResponseCacheUpdateIntervalMs(long responseCacheUpdateIntervalMs) {
		this.responseCacheUpdateIntervalMs = responseCacheUpdateIntervalMs;
	}

	public void setUseReadOnlyResponseCache(boolean useReadOnlyResponseCache) {
		this.useReadOnlyResponseCache = useReadOnlyResponseCache;
	}

	public void setDisableDelta(boolean disableDelta) {
		this.disableDelta = disableDelta;
	}

	public void setMaxIdleThreadInMinutesAgeForStatusReplication(
			long maxIdleThreadInMinutesAgeForStatusReplication) {
		this.maxIdleThreadInMinutesAgeForStatusReplication = maxIdleThreadInMinutesAgeForStatusReplication;
	}

	public void setMinThreadsForStatusReplication(int minThreadsForStatusReplication) {
		this.minThreadsForStatusReplication = minThreadsForStatusReplication;
	}

	public void setMaxThreadsForStatusReplication(int maxThreadsForStatusReplication) {
		this.maxThreadsForStatusReplication = maxThreadsForStatusReplication;
	}

	public void setMaxElementsInStatusReplicationPool(
			int maxElementsInStatusReplicationPool) {
		this.maxElementsInStatusReplicationPool = maxElementsInStatusReplicationPool;
	}

	public void setSyncWhenTimestampDiffers(boolean syncWhenTimestampDiffers) {
		this.syncWhenTimestampDiffers = syncWhenTimestampDiffers;
	}

	public void setRegistrySyncRetries(int registrySyncRetries) {
		this.registrySyncRetries = registrySyncRetries;
	}

	public void setRegistrySyncRetryWaitMs(long registrySyncRetryWaitMs) {
		this.registrySyncRetryWaitMs = registrySyncRetryWaitMs;
	}

	public void setMaxElementsInPeerReplicationPool(
			int maxElementsInPeerReplicationPool) {
		this.maxElementsInPeerReplicationPool = maxElementsInPeerReplicationPool;
	}

	public void setMaxIdleThreadAgeInMinutesForPeerReplication(
			long maxIdleThreadAgeInMinutesForPeerReplication) {
		this.maxIdleThreadAgeInMinutesForPeerReplication = maxIdleThreadAgeInMinutesForPeerReplication;
	}

	public void setMinThreadsForPeerReplication(int minThreadsForPeerReplication) {
		this.minThreadsForPeerReplication = minThreadsForPeerReplication;
	}

	public void setMaxThreadsForPeerReplication(int maxThreadsForPeerReplication) {
		this.maxThreadsForPeerReplication = maxThreadsForPeerReplication;
	}

	public void setMaxTimeForReplication(int maxTimeForReplication) {
		this.maxTimeForReplication = maxTimeForReplication;
	}

	public void setPrimeAwsReplicaConnections(boolean primeAwsReplicaConnections) {
		this.primeAwsReplicaConnections = primeAwsReplicaConnections;
	}

	public void setDisableDeltaForRemoteRegions(boolean disableDeltaForRemoteRegions) {
		this.disableDeltaForRemoteRegions = disableDeltaForRemoteRegions;
	}

	public void setRemoteRegionConnectTimeoutMs(int remoteRegionConnectTimeoutMs) {
		this.remoteRegionConnectTimeoutMs = remoteRegionConnectTimeoutMs;
	}

	public void setRemoteRegionReadTimeoutMs(int remoteRegionReadTimeoutMs) {
		this.remoteRegionReadTimeoutMs = remoteRegionReadTimeoutMs;
	}

	public void setRemoteRegionTotalConnections(int remoteRegionTotalConnections) {
		this.remoteRegionTotalConnections = remoteRegionTotalConnections;
	}

	public void setRemoteRegionTotalConnectionsPerHost(
			int remoteRegionTotalConnectionsPerHost) {
		this.remoteRegionTotalConnectionsPerHost = remoteRegionTotalConnectionsPerHost;
	}

	public void setRemoteRegionConnectionIdleTimeoutSeconds(
			int remoteRegionConnectionIdleTimeoutSeconds) {
		this.remoteRegionConnectionIdleTimeoutSeconds = remoteRegionConnectionIdleTimeoutSeconds;
	}

	public void setGZipContentFromRemoteRegion(boolean gZipContentFromRemoteRegion) {
		this.gZipContentFromRemoteRegion = gZipContentFromRemoteRegion;
	}

	public void setRemoteRegionUrlsWithName(
			Map<String, String> remoteRegionUrlsWithName) {
		this.remoteRegionUrlsWithName = remoteRegionUrlsWithName;
	}

	public void setRemoteRegionUrls(String[] remoteRegionUrls) {
		this.remoteRegionUrls = remoteRegionUrls;
	}

	public void setRemoteRegionAppWhitelist(
			Map<String, Set<String>> remoteRegionAppWhitelist) {
		this.remoteRegionAppWhitelist = remoteRegionAppWhitelist;
	}

	public void setRemoteRegionRegistryFetchInterval(
			int remoteRegionRegistryFetchInterval) {
		this.remoteRegionRegistryFetchInterval = remoteRegionRegistryFetchInterval;
	}

	public void setRemoteRegionFetchThreadPoolSize(int remoteRegionFetchThreadPoolSize) {
		this.remoteRegionFetchThreadPoolSize = remoteRegionFetchThreadPoolSize;
	}

	public void setRemoteRegionTrustStore(String remoteRegionTrustStore) {
		this.remoteRegionTrustStore = remoteRegionTrustStore;
	}

	public void setRemoteRegionTrustStorePassword(String remoteRegionTrustStorePassword) {
		this.remoteRegionTrustStorePassword = remoteRegionTrustStorePassword;
	}

	public void setDisableTransparentFallbackToOtherRegion(
			boolean disableTransparentFallbackToOtherRegion) {
		this.disableTransparentFallbackToOtherRegion = disableTransparentFallbackToOtherRegion;
	}

	public void setBatchReplication(boolean batchReplication) {
		this.batchReplication = batchReplication;
	}

	public void setRateLimiterEnabled(boolean rateLimiterEnabled) {
		this.rateLimiterEnabled = rateLimiterEnabled;
	}

	public void setRateLimiterThrottleStandardClients(
			boolean rateLimiterThrottleStandardClients) {
		this.rateLimiterThrottleStandardClients = rateLimiterThrottleStandardClients;
	}

	public void setRateLimiterPrivilegedClients(
			Set<String> rateLimiterPrivilegedClients) {
		this.rateLimiterPrivilegedClients = rateLimiterPrivilegedClients;
	}

	public void setRateLimiterBurstSize(int rateLimiterBurstSize) {
		this.rateLimiterBurstSize = rateLimiterBurstSize;
	}

	public void setRateLimiterRegistryFetchAverageRate(
			int rateLimiterRegistryFetchAverageRate) {
		this.rateLimiterRegistryFetchAverageRate = rateLimiterRegistryFetchAverageRate;
	}

	public void setRateLimiterFullFetchAverageRate(int rateLimiterFullFetchAverageRate) {
		this.rateLimiterFullFetchAverageRate = rateLimiterFullFetchAverageRate;
	}

	public void setLogIdentityHeaders(boolean logIdentityHeaders) {
		this.logIdentityHeaders = logIdentityHeaders;
	}

	public void setListAutoScalingGroupsRoleName(String listAutoScalingGroupsRoleName) {
		this.listAutoScalingGroupsRoleName = listAutoScalingGroupsRoleName;
	}

	public void setEnableReplicatedRequestCompression(
			boolean enableReplicatedRequestCompression) {
		this.enableReplicatedRequestCompression = enableReplicatedRequestCompression;
	}

	public void setJsonCodecName(String jsonCodecName) {
		this.jsonCodecName = jsonCodecName;
	}

	public void setXmlCodecName(String xmlCodecName) {
		this.xmlCodecName = xmlCodecName;
	}

	public void setRoute53BindRebindRetries(int route53BindRebindRetries) {
		this.route53BindRebindRetries = route53BindRebindRetries;
	}

	public void setRoute53BindingRetryIntervalMs(int route53BindingRetryIntervalMs) {
		this.route53BindingRetryIntervalMs = route53BindingRetryIntervalMs;
	}

	public void setRoute53DomainTTL(long route53DomainTTL) {
		this.route53DomainTTL = route53DomainTTL;
	}

	public void setBindingStrategy(AwsBindingStrategy bindingStrategy) {
		this.bindingStrategy = bindingStrategy;
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof EurekaServerConfigBean))
			return false;
		final EurekaServerConfigBean other = (EurekaServerConfigBean) o;
		if (!other.canEqual((Object) this))
			return false;
		final Object this$propertyResolver = this.propertyResolver;
		final Object other$propertyResolver = other.propertyResolver;
		if (this$propertyResolver == null ?
				other$propertyResolver != null :
				!this$propertyResolver.equals(other$propertyResolver))
			return false;
		final Object this$aWSAccessId = this.aWSAccessId;
		final Object other$aWSAccessId = other.aWSAccessId;
		if (this$aWSAccessId == null ?
				other$aWSAccessId != null :
				!this$aWSAccessId.equals(other$aWSAccessId))
			return false;
		final Object this$aWSSecretKey = this.aWSSecretKey;
		final Object other$aWSSecretKey = other.aWSSecretKey;
		if (this$aWSSecretKey == null ?
				other$aWSSecretKey != null :
				!this$aWSSecretKey.equals(other$aWSSecretKey))
			return false;
		if (this.eIPBindRebindRetries != other.eIPBindRebindRetries)
			return false;
		if (this.eIPBindingRetryIntervalMs != other.eIPBindingRetryIntervalMs)
			return false;
		if (this.eIPBindingRetryIntervalMsWhenUnbound
				!= other.eIPBindingRetryIntervalMsWhenUnbound)
			return false;
		if (this.enableSelfPreservation != other.enableSelfPreservation)
			return false;
		if (Double.compare(this.renewalPercentThreshold, other.renewalPercentThreshold)
				!= 0)
			return false;
		if (this.renewalThresholdUpdateIntervalMs
				!= other.renewalThresholdUpdateIntervalMs)
			return false;
		if (this.peerEurekaNodesUpdateIntervalMs != other.peerEurekaNodesUpdateIntervalMs)
			return false;
		if (this.numberOfReplicationRetries != other.numberOfReplicationRetries)
			return false;
		if (this.peerEurekaStatusRefreshTimeIntervalMs
				!= other.peerEurekaStatusRefreshTimeIntervalMs)
			return false;
		if (this.waitTimeInMsWhenSyncEmpty != other.waitTimeInMsWhenSyncEmpty)
			return false;
		if (this.peerNodeConnectTimeoutMs != other.peerNodeConnectTimeoutMs)
			return false;
		if (this.peerNodeReadTimeoutMs != other.peerNodeReadTimeoutMs)
			return false;
		if (this.peerNodeTotalConnections != other.peerNodeTotalConnections)
			return false;
		if (this.peerNodeTotalConnectionsPerHost != other.peerNodeTotalConnectionsPerHost)
			return false;
		if (this.peerNodeConnectionIdleTimeoutSeconds
				!= other.peerNodeConnectionIdleTimeoutSeconds)
			return false;
		if (this.retentionTimeInMSInDeltaQueue != other.retentionTimeInMSInDeltaQueue)
			return false;
		if (this.deltaRetentionTimerIntervalInMs != other.deltaRetentionTimerIntervalInMs)
			return false;
		if (this.evictionIntervalTimerInMs != other.evictionIntervalTimerInMs)
			return false;
		if (this.aSGQueryTimeoutMs != other.aSGQueryTimeoutMs)
			return false;
		if (this.aSGUpdateIntervalMs != other.aSGUpdateIntervalMs)
			return false;
		if (this.aSGCacheExpiryTimeoutMs != other.aSGCacheExpiryTimeoutMs)
			return false;
		if (this.responseCacheAutoExpirationInSeconds
				!= other.responseCacheAutoExpirationInSeconds)
			return false;
		if (this.responseCacheUpdateIntervalMs != other.responseCacheUpdateIntervalMs)
			return false;
		if (this.useReadOnlyResponseCache != other.useReadOnlyResponseCache)
			return false;
		if (this.disableDelta != other.disableDelta)
			return false;
		if (this.maxIdleThreadInMinutesAgeForStatusReplication
				!= other.maxIdleThreadInMinutesAgeForStatusReplication)
			return false;
		if (this.minThreadsForStatusReplication != other.minThreadsForStatusReplication)
			return false;
		if (this.maxThreadsForStatusReplication != other.maxThreadsForStatusReplication)
			return false;
		if (this.maxElementsInStatusReplicationPool
				!= other.maxElementsInStatusReplicationPool)
			return false;
		if (this.syncWhenTimestampDiffers != other.syncWhenTimestampDiffers)
			return false;
		if (this.registrySyncRetries != other.registrySyncRetries)
			return false;
		if (this.registrySyncRetryWaitMs != other.registrySyncRetryWaitMs)
			return false;
		if (this.maxElementsInPeerReplicationPool
				!= other.maxElementsInPeerReplicationPool)
			return false;
		if (this.maxIdleThreadAgeInMinutesForPeerReplication
				!= other.maxIdleThreadAgeInMinutesForPeerReplication)
			return false;
		if (this.minThreadsForPeerReplication != other.minThreadsForPeerReplication)
			return false;
		if (this.maxThreadsForPeerReplication != other.maxThreadsForPeerReplication)
			return false;
		if (this.maxTimeForReplication != other.maxTimeForReplication)
			return false;
		if (this.primeAwsReplicaConnections != other.primeAwsReplicaConnections)
			return false;
		if (this.disableDeltaForRemoteRegions != other.disableDeltaForRemoteRegions)
			return false;
		if (this.remoteRegionConnectTimeoutMs != other.remoteRegionConnectTimeoutMs)
			return false;
		if (this.remoteRegionReadTimeoutMs != other.remoteRegionReadTimeoutMs)
			return false;
		if (this.remoteRegionTotalConnections != other.remoteRegionTotalConnections)
			return false;
		if (this.remoteRegionTotalConnectionsPerHost
				!= other.remoteRegionTotalConnectionsPerHost)
			return false;
		if (this.remoteRegionConnectionIdleTimeoutSeconds
				!= other.remoteRegionConnectionIdleTimeoutSeconds)
			return false;
		if (this.gZipContentFromRemoteRegion != other.gZipContentFromRemoteRegion)
			return false;
		final Object this$remoteRegionUrlsWithName = this.remoteRegionUrlsWithName;
		final Object other$remoteRegionUrlsWithName = other.remoteRegionUrlsWithName;
		if (this$remoteRegionUrlsWithName == null ?
				other$remoteRegionUrlsWithName != null :
				!this$remoteRegionUrlsWithName.equals(other$remoteRegionUrlsWithName))
			return false;
		if (!java.util.Arrays.deepEquals(this.remoteRegionUrls, other.remoteRegionUrls))
			return false;
		final Object this$remoteRegionAppWhitelist = this.getRemoteRegionAppWhitelist();
		final Object other$remoteRegionAppWhitelist = other.getRemoteRegionAppWhitelist();
		if (this$remoteRegionAppWhitelist == null ?
				other$remoteRegionAppWhitelist != null :
				!this$remoteRegionAppWhitelist.equals(other$remoteRegionAppWhitelist))
			return false;
		if (this.remoteRegionRegistryFetchInterval
				!= other.remoteRegionRegistryFetchInterval)
			return false;
		if (this.remoteRegionFetchThreadPoolSize != other.remoteRegionFetchThreadPoolSize)
			return false;
		final Object this$remoteRegionTrustStore = this.remoteRegionTrustStore;
		final Object other$remoteRegionTrustStore = other.remoteRegionTrustStore;
		if (this$remoteRegionTrustStore == null ?
				other$remoteRegionTrustStore != null :
				!this$remoteRegionTrustStore.equals(other$remoteRegionTrustStore))
			return false;
		final Object this$remoteRegionTrustStorePassword = this.remoteRegionTrustStorePassword;
		final Object other$remoteRegionTrustStorePassword = other.remoteRegionTrustStorePassword;
		if (this$remoteRegionTrustStorePassword == null ?
				other$remoteRegionTrustStorePassword != null :
				!this$remoteRegionTrustStorePassword
						.equals(other$remoteRegionTrustStorePassword))
			return false;
		if (this.disableTransparentFallbackToOtherRegion
				!= other.disableTransparentFallbackToOtherRegion)
			return false;
		if (this.batchReplication != other.batchReplication)
			return false;
		if (this.rateLimiterEnabled != other.rateLimiterEnabled)
			return false;
		if (this.rateLimiterThrottleStandardClients
				!= other.rateLimiterThrottleStandardClients)
			return false;
		final Object this$rateLimiterPrivilegedClients = this.rateLimiterPrivilegedClients;
		final Object other$rateLimiterPrivilegedClients = other.rateLimiterPrivilegedClients;
		if (this$rateLimiterPrivilegedClients == null ?
				other$rateLimiterPrivilegedClients != null :
				!this$rateLimiterPrivilegedClients
						.equals(other$rateLimiterPrivilegedClients))
			return false;
		if (this.rateLimiterBurstSize != other.rateLimiterBurstSize)
			return false;
		if (this.rateLimiterRegistryFetchAverageRate
				!= other.rateLimiterRegistryFetchAverageRate)
			return false;
		if (this.rateLimiterFullFetchAverageRate != other.rateLimiterFullFetchAverageRate)
			return false;
		if (this.logIdentityHeaders != other.logIdentityHeaders)
			return false;
		final Object this$listAutoScalingGroupsRoleName = this.listAutoScalingGroupsRoleName;
		final Object other$listAutoScalingGroupsRoleName = other.listAutoScalingGroupsRoleName;
		if (this$listAutoScalingGroupsRoleName == null ?
				other$listAutoScalingGroupsRoleName != null :
				!this$listAutoScalingGroupsRoleName
						.equals(other$listAutoScalingGroupsRoleName))
			return false;
		if (this.enableReplicatedRequestCompression
				!= other.enableReplicatedRequestCompression)
			return false;
		final Object this$jsonCodecName = this.getJsonCodecName();
		final Object other$jsonCodecName = other.getJsonCodecName();
		if (this$jsonCodecName == null ?
				other$jsonCodecName != null :
				!this$jsonCodecName.equals(other$jsonCodecName))
			return false;
		final Object this$xmlCodecName = this.getXmlCodecName();
		final Object other$xmlCodecName = other.getXmlCodecName();
		if (this$xmlCodecName == null ?
				other$xmlCodecName != null :
				!this$xmlCodecName.equals(other$xmlCodecName))
			return false;
		if (this.route53BindRebindRetries != other.route53BindRebindRetries)
			return false;
		if (this.route53BindingRetryIntervalMs != other.route53BindingRetryIntervalMs)
			return false;
		if (this.route53DomainTTL != other.route53DomainTTL)
			return false;
		final Object this$bindingStrategy = this.bindingStrategy;
		final Object other$bindingStrategy = other.bindingStrategy;
		if (this$bindingStrategy == null ?
				other$bindingStrategy != null :
				!this$bindingStrategy.equals(other$bindingStrategy))
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
		final Object $aWSAccessId = this.aWSAccessId;
		result = result * PRIME + ($aWSAccessId == null ? 0 : $aWSAccessId.hashCode());
		final Object $aWSSecretKey = this.aWSSecretKey;
		result = result * PRIME + ($aWSSecretKey == null ? 0 : $aWSSecretKey.hashCode());
		result = result * PRIME + this.eIPBindRebindRetries;
		result = result * PRIME + this.eIPBindingRetryIntervalMs;
		result = result * PRIME + this.eIPBindingRetryIntervalMsWhenUnbound;
		result = result * PRIME + (this.enableSelfPreservation ? 79 : 97);
		final long $renewalPercentThreshold = Double
				.doubleToLongBits(this.renewalPercentThreshold);
		result = result * PRIME + (int) ($renewalPercentThreshold >>> 32
				^ $renewalPercentThreshold);
		result = result * PRIME + this.renewalThresholdUpdateIntervalMs;
		result = result * PRIME + this.peerEurekaNodesUpdateIntervalMs;
		result = result * PRIME + this.numberOfReplicationRetries;
		result = result * PRIME + this.peerEurekaStatusRefreshTimeIntervalMs;
		result = result * PRIME + this.waitTimeInMsWhenSyncEmpty;
		result = result * PRIME + this.peerNodeConnectTimeoutMs;
		result = result * PRIME + this.peerNodeReadTimeoutMs;
		result = result * PRIME + this.peerNodeTotalConnections;
		result = result * PRIME + this.peerNodeTotalConnectionsPerHost;
		result = result * PRIME + this.peerNodeConnectionIdleTimeoutSeconds;
		final long $retentionTimeInMSInDeltaQueue = this.retentionTimeInMSInDeltaQueue;
		result = result * PRIME + (int) ($retentionTimeInMSInDeltaQueue >>> 32
				^ $retentionTimeInMSInDeltaQueue);
		final long $deltaRetentionTimerIntervalInMs = this.deltaRetentionTimerIntervalInMs;
		result = result * PRIME + (int) ($deltaRetentionTimerIntervalInMs >>> 32
				^ $deltaRetentionTimerIntervalInMs);
		final long $evictionIntervalTimerInMs = this.evictionIntervalTimerInMs;
		result = result * PRIME + (int) ($evictionIntervalTimerInMs >>> 32
				^ $evictionIntervalTimerInMs);
		result = result * PRIME + this.aSGQueryTimeoutMs;
		final long $aSGUpdateIntervalMs = this.aSGUpdateIntervalMs;
		result = result * PRIME + (int) ($aSGUpdateIntervalMs >>> 32
				^ $aSGUpdateIntervalMs);
		final long $aSGCacheExpiryTimeoutMs = this.aSGCacheExpiryTimeoutMs;
		result = result * PRIME + (int) ($aSGCacheExpiryTimeoutMs >>> 32
				^ $aSGCacheExpiryTimeoutMs);
		final long $responseCacheAutoExpirationInSeconds = this.responseCacheAutoExpirationInSeconds;
		result = result * PRIME + (int) ($responseCacheAutoExpirationInSeconds >>> 32
				^ $responseCacheAutoExpirationInSeconds);
		final long $responseCacheUpdateIntervalMs = this.responseCacheUpdateIntervalMs;
		result = result * PRIME + (int) ($responseCacheUpdateIntervalMs >>> 32
				^ $responseCacheUpdateIntervalMs);
		result = result * PRIME + (this.useReadOnlyResponseCache ? 79 : 97);
		result = result * PRIME + (this.disableDelta ? 79 : 97);
		final long $maxIdleThreadInMinutesAgeForStatusReplication = this.maxIdleThreadInMinutesAgeForStatusReplication;
		result = result * PRIME + (int) (
				$maxIdleThreadInMinutesAgeForStatusReplication >>> 32
						^ $maxIdleThreadInMinutesAgeForStatusReplication);
		result = result * PRIME + this.minThreadsForStatusReplication;
		result = result * PRIME + this.maxThreadsForStatusReplication;
		result = result * PRIME + this.maxElementsInStatusReplicationPool;
		result = result * PRIME + (this.syncWhenTimestampDiffers ? 79 : 97);
		result = result * PRIME + this.registrySyncRetries;
		final long $registrySyncRetryWaitMs = this.registrySyncRetryWaitMs;
		result = result * PRIME + (int) ($registrySyncRetryWaitMs >>> 32
				^ $registrySyncRetryWaitMs);
		result = result * PRIME + this.maxElementsInPeerReplicationPool;
		final long $maxIdleThreadAgeInMinutesForPeerReplication = this.maxIdleThreadAgeInMinutesForPeerReplication;
		result = result * PRIME + (int) (
				$maxIdleThreadAgeInMinutesForPeerReplication >>> 32
						^ $maxIdleThreadAgeInMinutesForPeerReplication);
		result = result * PRIME + this.minThreadsForPeerReplication;
		result = result * PRIME + this.maxThreadsForPeerReplication;
		result = result * PRIME + this.maxTimeForReplication;
		result = result * PRIME + (this.primeAwsReplicaConnections ? 79 : 97);
		result = result * PRIME + (this.disableDeltaForRemoteRegions ? 79 : 97);
		result = result * PRIME + this.remoteRegionConnectTimeoutMs;
		result = result * PRIME + this.remoteRegionReadTimeoutMs;
		result = result * PRIME + this.remoteRegionTotalConnections;
		result = result * PRIME + this.remoteRegionTotalConnectionsPerHost;
		result = result * PRIME + this.remoteRegionConnectionIdleTimeoutSeconds;
		result = result * PRIME + (this.gZipContentFromRemoteRegion ? 79 : 97);
		final Object $remoteRegionUrlsWithName = this.remoteRegionUrlsWithName;
		result = result * PRIME + ($remoteRegionUrlsWithName == null ?
				0 :
				$remoteRegionUrlsWithName.hashCode());
		result = result * PRIME + java.util.Arrays.deepHashCode(this.remoteRegionUrls);
		final Object $remoteRegionAppWhitelist = this.getRemoteRegionAppWhitelist();
		result = result * PRIME + ($remoteRegionAppWhitelist == null ?
				0 :
				$remoteRegionAppWhitelist.hashCode());
		result = result * PRIME + this.remoteRegionRegistryFetchInterval;
		result = result * PRIME + this.remoteRegionFetchThreadPoolSize;
		final Object $remoteRegionTrustStore = this.remoteRegionTrustStore;
		result = result * PRIME + ($remoteRegionTrustStore == null ?
				0 :
				$remoteRegionTrustStore.hashCode());
		final Object $remoteRegionTrustStorePassword = this.remoteRegionTrustStorePassword;
		result = result * PRIME + ($remoteRegionTrustStorePassword == null ?
				0 :
				$remoteRegionTrustStorePassword.hashCode());
		result =
				result * PRIME + (this.disableTransparentFallbackToOtherRegion ? 79 : 97);
		result = result * PRIME + (this.batchReplication ? 79 : 97);
		result = result * PRIME + (this.rateLimiterEnabled ? 79 : 97);
		result = result * PRIME + (this.rateLimiterThrottleStandardClients ? 79 : 97);
		final Object $rateLimiterPrivilegedClients = this.rateLimiterPrivilegedClients;
		result = result * PRIME + ($rateLimiterPrivilegedClients == null ?
				0 :
				$rateLimiterPrivilegedClients.hashCode());
		result = result * PRIME + this.rateLimiterBurstSize;
		result = result * PRIME + this.rateLimiterRegistryFetchAverageRate;
		result = result * PRIME + this.rateLimiterFullFetchAverageRate;
		result = result * PRIME + (this.logIdentityHeaders ? 79 : 97);
		final Object $listAutoScalingGroupsRoleName = this.listAutoScalingGroupsRoleName;
		result = result * PRIME + ($listAutoScalingGroupsRoleName == null ?
				0 :
				$listAutoScalingGroupsRoleName.hashCode());
		result = result * PRIME + (this.enableReplicatedRequestCompression ? 79 : 97);
		final Object $jsonCodecName = this.getJsonCodecName();
		result =
				result * PRIME + ($jsonCodecName == null ? 0 : $jsonCodecName.hashCode());
		final Object $xmlCodecName = this.getXmlCodecName();
		result = result * PRIME + ($xmlCodecName == null ? 0 : $xmlCodecName.hashCode());
		result = result * PRIME + this.route53BindRebindRetries;
		result = result * PRIME + this.route53BindingRetryIntervalMs;
		final long $route53DomainTTL = this.route53DomainTTL;
		result = result * PRIME + (int) ($route53DomainTTL >>> 32 ^ $route53DomainTTL);
		final Object $bindingStrategy = this.bindingStrategy;
		result = result * PRIME + ($bindingStrategy == null ?
				0 :
				$bindingStrategy.hashCode());
		return result;
	}

	protected boolean canEqual(Object other) {
		return other instanceof EurekaServerConfigBean;
	}

	public String toString() {
		return "org.springframework.cloud.netflix.eureka.server.EurekaServerConfigBean(propertyResolver="
				+ this.propertyResolver + ", aWSAccessId=" + this.aWSAccessId
				+ ", aWSSecretKey=" + this.aWSSecretKey + ", eIPBindRebindRetries="
				+ this.eIPBindRebindRetries + ", eIPBindingRetryIntervalMs="
				+ this.eIPBindingRetryIntervalMs
				+ ", eIPBindingRetryIntervalMsWhenUnbound="
				+ this.eIPBindingRetryIntervalMsWhenUnbound + ", enableSelfPreservation="
				+ this.enableSelfPreservation + ", renewalPercentThreshold="
				+ this.renewalPercentThreshold + ", renewalThresholdUpdateIntervalMs="
				+ this.renewalThresholdUpdateIntervalMs
				+ ", peerEurekaNodesUpdateIntervalMs="
				+ this.peerEurekaNodesUpdateIntervalMs + ", numberOfReplicationRetries="
				+ this.numberOfReplicationRetries
				+ ", peerEurekaStatusRefreshTimeIntervalMs="
				+ this.peerEurekaStatusRefreshTimeIntervalMs
				+ ", waitTimeInMsWhenSyncEmpty=" + this.waitTimeInMsWhenSyncEmpty
				+ ", peerNodeConnectTimeoutMs=" + this.peerNodeConnectTimeoutMs
				+ ", peerNodeReadTimeoutMs=" + this.peerNodeReadTimeoutMs
				+ ", peerNodeTotalConnections=" + this.peerNodeTotalConnections
				+ ", peerNodeTotalConnectionsPerHost="
				+ this.peerNodeTotalConnectionsPerHost
				+ ", peerNodeConnectionIdleTimeoutSeconds="
				+ this.peerNodeConnectionIdleTimeoutSeconds
				+ ", retentionTimeInMSInDeltaQueue=" + this.retentionTimeInMSInDeltaQueue
				+ ", deltaRetentionTimerIntervalInMs="
				+ this.deltaRetentionTimerIntervalInMs + ", evictionIntervalTimerInMs="
				+ this.evictionIntervalTimerInMs + ", aSGQueryTimeoutMs="
				+ this.aSGQueryTimeoutMs + ", aSGUpdateIntervalMs="
				+ this.aSGUpdateIntervalMs + ", aSGCacheExpiryTimeoutMs="
				+ this.aSGCacheExpiryTimeoutMs + ", responseCacheAutoExpirationInSeconds="
				+ this.responseCacheAutoExpirationInSeconds
				+ ", responseCacheUpdateIntervalMs=" + this.responseCacheUpdateIntervalMs
				+ ", useReadOnlyResponseCache=" + this.useReadOnlyResponseCache
				+ ", disableDelta=" + this.disableDelta
				+ ", maxIdleThreadInMinutesAgeForStatusReplication="
				+ this.maxIdleThreadInMinutesAgeForStatusReplication
				+ ", minThreadsForStatusReplication="
				+ this.minThreadsForStatusReplication
				+ ", maxThreadsForStatusReplication="
				+ this.maxThreadsForStatusReplication
				+ ", maxElementsInStatusReplicationPool="
				+ this.maxElementsInStatusReplicationPool + ", syncWhenTimestampDiffers="
				+ this.syncWhenTimestampDiffers + ", registrySyncRetries="
				+ this.registrySyncRetries + ", registrySyncRetryWaitMs="
				+ this.registrySyncRetryWaitMs + ", maxElementsInPeerReplicationPool="
				+ this.maxElementsInPeerReplicationPool
				+ ", maxIdleThreadAgeInMinutesForPeerReplication="
				+ this.maxIdleThreadAgeInMinutesForPeerReplication
				+ ", minThreadsForPeerReplication=" + this.minThreadsForPeerReplication
				+ ", maxThreadsForPeerReplication=" + this.maxThreadsForPeerReplication
				+ ", maxTimeForReplication=" + this.maxTimeForReplication
				+ ", primeAwsReplicaConnections=" + this.primeAwsReplicaConnections
				+ ", disableDeltaForRemoteRegions=" + this.disableDeltaForRemoteRegions
				+ ", remoteRegionConnectTimeoutMs=" + this.remoteRegionConnectTimeoutMs
				+ ", remoteRegionReadTimeoutMs=" + this.remoteRegionReadTimeoutMs
				+ ", remoteRegionTotalConnections=" + this.remoteRegionTotalConnections
				+ ", remoteRegionTotalConnectionsPerHost="
				+ this.remoteRegionTotalConnectionsPerHost
				+ ", remoteRegionConnectionIdleTimeoutSeconds="
				+ this.remoteRegionConnectionIdleTimeoutSeconds
				+ ", gZipContentFromRemoteRegion=" + this.gZipContentFromRemoteRegion
				+ ", remoteRegionUrlsWithName=" + this.remoteRegionUrlsWithName
				+ ", remoteRegionUrls=" + java.util.Arrays
				.deepToString(this.remoteRegionUrls) + ", remoteRegionAppWhitelist="
				+ this.getRemoteRegionAppWhitelist()
				+ ", remoteRegionRegistryFetchInterval="
				+ this.remoteRegionRegistryFetchInterval
				+ ", remoteRegionFetchThreadPoolSize="
				+ this.remoteRegionFetchThreadPoolSize + ", remoteRegionTrustStore="
				+ this.remoteRegionTrustStore + ", remoteRegionTrustStorePassword="
				+ this.remoteRegionTrustStorePassword
				+ ", disableTransparentFallbackToOtherRegion="
				+ this.disableTransparentFallbackToOtherRegion + ", batchReplication="
				+ this.batchReplication + ", rateLimiterEnabled="
				+ this.rateLimiterEnabled + ", rateLimiterThrottleStandardClients="
				+ this.rateLimiterThrottleStandardClients
				+ ", rateLimiterPrivilegedClients=" + this.rateLimiterPrivilegedClients
				+ ", rateLimiterBurstSize=" + this.rateLimiterBurstSize
				+ ", rateLimiterRegistryFetchAverageRate="
				+ this.rateLimiterRegistryFetchAverageRate
				+ ", rateLimiterFullFetchAverageRate="
				+ this.rateLimiterFullFetchAverageRate + ", logIdentityHeaders="
				+ this.logIdentityHeaders + ", listAutoScalingGroupsRoleName="
				+ this.listAutoScalingGroupsRoleName
				+ ", enableReplicatedRequestCompression="
				+ this.enableReplicatedRequestCompression + ", jsonCodecName=" + this
				.getJsonCodecName() + ", xmlCodecName=" + this.getXmlCodecName()
				+ ", route53BindRebindRetries=" + this.route53BindRebindRetries
				+ ", route53BindingRetryIntervalMs=" + this.route53BindingRetryIntervalMs
				+ ", route53DomainTTL=" + this.route53DomainTTL + ", bindingStrategy="
				+ this.bindingStrategy + ")";
	}
}
