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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.appinfo.UniqueIdentifier;

/**
 * @author Dave Syer
 */
@Data
@ConfigurationProperties("eureka.instance")
public class EurekaInstanceConfigBean implements EurekaInstanceConfig {

	private static final Log logger = LogFactory.getLog(EurekaInstanceConfigBean.class);

	@Getter(AccessLevel.PRIVATE)
	@Setter(AccessLevel.PRIVATE)
	private HostInfo hostInfo = initHostInfo();

	@Value("${spring.application.name:unknown}")
	private String appname = "unknown";

	private String appGroupName;

	private boolean instanceEnabledOnit;

	private int nonSecurePort = 80;

	private int securePort = 443;

	private boolean nonSecurePortEnabled = true;

	private boolean securePortEnabled;

	private int leaseRenewalIntervalInSeconds = 30;

	private int leaseExpirationDurationInSeconds = 90;

	@Value("${spring.application.name:unknown}")
	private String virtualHostName;

	private String secureVirtualHostName;

	private String aSGName;

	private Map<String, String> metadataMap = new HashMap<>();

	private DataCenterInfo dataCenterInfo = new IdentifyingDataCenterInfo();

	private String ipAddress = this.hostInfo.ipAddress;

	private String statusPageUrlPath = "/info";

	private String statusPageUrl;

	private String homePageUrlPath = "/";

	private String homePageUrl;

	private String healthCheckUrlPath = "/health";

	private String healthCheckUrl;

	private String secureHealthCheckUrl;

	private String namespace = "eureka";

	private String hostname = this.hostInfo.hostname;

	private boolean preferIpAddress = false;

	private InstanceStatus initialStatus = InstanceStatus.UP;

	public String getHostname() {
		return getHostName(false);
	}

	@Override
	public boolean getSecurePortEnabled() {
		return this.securePortEnabled;
	}

	private HostInfo initHostInfo() {
		HostInfo info = new HostInfo();
		try {
			info.ipAddress = InetAddress.getLocalHost().getHostAddress();
			info.hostname = InetAddress.getLocalHost().getHostName();
		}
		catch (UnknownHostException ex) {
			logger.error("Cannot get host info", ex);
		}
		return info;
	}

	@Override
	public String getHostName(boolean refresh) {
		if (refresh) {
			this.hostInfo = initHostInfo();
			this.ipAddress = this.hostInfo.ipAddress;
			this.hostname = this.hostInfo.hostname;
		}
		return this.preferIpAddress ? this.ipAddress : this.hostname;
	}

	private final class HostInfo {
		private String ipAddress;
		private String hostname;
	}

	private final class IdentifyingDataCenterInfo implements DataCenterInfo,
			UniqueIdentifier {

		@Getter
		@Setter
		private Name name = Name.MyOwn;

		@Override
		public String getId() {
			String instanceId = EurekaInstanceConfigBean.this.metadataMap
					.get("instanceId");
			if (instanceId != null) {
				String old = getHostname();
				String id = old.endsWith(instanceId) ? old : old + ":" + instanceId;
				return id;
			}
			return getHostname();
		}

	}

}
