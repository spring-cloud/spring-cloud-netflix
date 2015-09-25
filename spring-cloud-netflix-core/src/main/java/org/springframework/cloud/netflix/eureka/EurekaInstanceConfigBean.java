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

import static org.springframework.cloud.util.InetUtils.getFirstNonLoopbackHostInfo;

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
import org.springframework.cloud.util.InetUtils.HostInfo;

import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.appinfo.MyDataCenterInfo;

/**
 * @author Dave Syer
 * @author Spencer Gibb
 */
@Data
@ConfigurationProperties("eureka.instance")
public class EurekaInstanceConfigBean implements EurekaInstanceConfig {

	private static final Log logger = LogFactory.getLog(EurekaInstanceConfigBean.class);

	@Getter(AccessLevel.PRIVATE)
	@Setter(AccessLevel.PRIVATE)
	private HostInfo hostInfo = getFirstNonLoopbackHostInfo();

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

    private String instanceId;

	private String secureVirtualHostName;

	private String aSGName;

	private Map<String, String> metadataMap = new HashMap<>();

	private DataCenterInfo dataCenterInfo = new MyDataCenterInfo(DataCenterInfo.Name.MyOwn);

	private String ipAddress = this.hostInfo.getIpAddress();

	private String statusPageUrlPath = "/info";

	private String statusPageUrl;

	private String homePageUrlPath = "/";

	private String homePageUrl;

	private String healthCheckUrlPath = "/health";

	private String healthCheckUrl;

	private String secureHealthCheckUrl;

	private String namespace = "eureka";

	private String hostname = this.hostInfo.getHostname();

	private boolean preferIpAddress = false;

	private InstanceStatus initialStatus = InstanceStatus.UP;

	public String getHostname() {
		return getHostName(false);
	}

    @Override
    public String getInstanceId() {
		if (this.instanceId == null && this.metadataMap != null) {
			return this.metadataMap.get("instanceId");
		}
        return instanceId;
    }

    @Override
	public boolean getSecurePortEnabled() {
		return this.securePortEnabled;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
		this.hostInfo.override = true;
	}

	@Override
	public String getHostName(boolean refresh) {
		if (refresh) {
			boolean originalOverride = this.hostInfo.override;
			this.hostInfo = getFirstNonLoopbackHostInfo();
			this.hostInfo.setOverride(originalOverride);
			this.ipAddress = this.hostInfo.getIpAddress();
			if (!this.hostInfo.override) {
				this.hostname = this.hostInfo.getHostname();
			}
		}
		return this.preferIpAddress ? this.ipAddress : this.hostname;
	}
}
