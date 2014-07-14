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
package org.springframework.platform.netflix.eureka;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

import com.netflix.eureka.EurekaServerConfig;

/**
 * @author Dave Syer
 *
 */
@Data
@ConfigurationProperties("eureka.server")
public class EurekaServerConfigBean implements EurekaServerConfig {

	private static final int MINUTES = 60 * 1000;

	private String aWSAccessId;

	private String aWSSecretKey;

	private int eIPBindRebindRetries = 3;

	private int eIPBindingRetryIntervalMs = 5 * MINUTES;

    private boolean enableSelfPreservation = true;
	@Override
	public boolean shouldEnableSelfPreservation() {
		return enableSelfPreservation;
	}

	private double renewalPercentThreshold = 0.85;

	private int renewalThresholdUpdateIntervalMs = 10 * MINUTES;

	private int peerEurekaNodesUpdateIntervalMs = 15 * MINUTES;

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

	private long responseCacheAutoExpirationInSeconds = 180;

	private long responseCacheUpdateIntervalMs = 30 * 1000;

    private boolean disableDelta;
	@Override
	public boolean shouldDisableDelta() {
		return disableDelta;
	}

	private long maxIdleThreadInMinutesAgeForStatusReplication = 10;

	private int minThreadsForStatusReplication = 1;

	private int maxThreadsForStatusReplication = 1;

	private int maxElementsInStatusReplicationPool = 10000;

    private boolean syncWhenTimestampDiffers = true;
	@Override
	public boolean shouldSyncWhenTimestampDiffers() {
		return syncWhenTimestampDiffers;
	}

	private int registrySyncRetries = 5;

	private int maxElementsInPeerReplicationPool = 10000;

	private long maxIdleThreadAgeInMinutesForPeerReplication = 15;

	private int minThreadsForPeerReplication = 5;

	private int maxThreadsForPeerReplication = 20;

	private int maxTimeForReplication = 30000;

    private boolean primeAwsReplicaConnections = true;
	@Override
	public boolean shouldPrimeAwsReplicaConnections() {
		return primeAwsReplicaConnections;
	}

    private boolean disableDeltaForRemoteRegions;
	@Override
	public boolean shouldDisableDeltaForRemoteRegions() {
		return disableDeltaForRemoteRegions;
	}

	private int remoteRegionConnectTimeoutMs = 1000;

	private int remoteRegionReadTimeoutMs = 1000;

	private int remoteRegionTotalConnections = 1000;

	private int remoteRegionTotalConnectionsPerHost = 500;

	private int remoteRegionConnectionIdleTimeoutSeconds = 30;

    private boolean gZipContentFromRemoteRegion = true;
	@Override
	public boolean shouldGZipContentFromRemoteRegion() {
		return gZipContentFromRemoteRegion;
	}

	private Map<String, String> remoteRegionUrlsWithName = new HashMap<String, String>();

	private String[] remoteRegionUrls;  

	private Map<String, Set<String>> remoteRegionAppWhitelist;
    @Override
	public Set<String> getRemoteRegionAppWhitelist(String regionName) {
        if (null == regionName) {
            regionName = "global";
        } else {
            regionName = regionName.trim().toLowerCase();
        }
		return remoteRegionAppWhitelist.get(regionName);
	}

	private int remoteRegionRegistryFetchInterval = 30;

	private String remoteRegionTrustStore = "";

	private String remoteRegionTrustStorePassword = "changeit";

	private boolean disableTransparentFallbackToOtherRegion;
	@Override
	public boolean disableTransparentFallbackToOtherRegion() {
		return disableTransparentFallbackToOtherRegion;
	}

    private boolean batchReplication;

	@Override
	public boolean shouldBatchReplication() {
		return batchReplication;
	}

}
