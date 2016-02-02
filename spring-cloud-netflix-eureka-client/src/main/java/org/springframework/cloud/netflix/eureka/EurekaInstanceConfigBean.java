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

import java.util.HashMap;
import java.util.Map;

import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.appinfo.MyDataCenterInfo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.util.InetUtils;
import org.springframework.cloud.util.InetUtils.HostInfo;

/**
 * @author Dave Syer
 * @author Spencer Gibb
 */
@ConfigurationProperties("eureka.instance")
public class EurekaInstanceConfigBean implements CloudEurekaInstanceConfig {

	private HostInfo hostInfo;

	private InetUtils inetUtils;

	/**
	 * Get the name of the application to be registered with eureka.
	 */
	@Value("${spring.application.name:unknown}")
	private String appname = "unknown";

	/**
	 * Get the name of the application group to be registered with eureka.
	 */
	private String appGroupName;

	/**
	 * Indicates whether the instance should be enabled for taking traffic as soon as it
	 * is registered with eureka. Sometimes the application might need to do some
	 * pre-processing before it is ready to take traffic.
	 */
	private boolean instanceEnabledOnit;

	/**
	 * Get the non-secure port on which the instance should receive traffic.
	 */
	private int nonSecurePort = 80;

	/**
	 * Get the Secure port on which the instance should receive traffic.
	 */
	private int securePort = 443;

	/**
	 * Indicates whether the non-secure port should be enabled for traffic or not.
	 */
	private boolean nonSecurePortEnabled = true;

	/**
	 * Indicates whether the secure port should be enabled for traffic or not.
	 */
	private boolean securePortEnabled;

	/**
	 * Indicates how often (in seconds) the eureka client needs to send heartbeats to
	 * eureka server to indicate that it is still alive. If the heartbeats are not
	 * received for the period specified in leaseExpirationDurationInSeconds, eureka
	 * server will remove the instance from its view, there by disallowing traffic to this
	 * instance.
	 *
	 * Note that the instance could still not take traffic if it implements
	 * HealthCheckCallback and then decides to make itself unavailable.
	 */
	private int leaseRenewalIntervalInSeconds = 30;

	/**
	 * Indicates the time in seconds that the eureka server waits since it received the
	 * last heartbeat before it can remove this instance from its view and there by
	 * disallowing traffic to this instance.
	 *
	 * Setting this value too long could mean that the traffic could be routed to the
	 * instance even though the instance is not alive. Setting this value too small could
	 * mean, the instance may be taken out of traffic because of temporary network
	 * glitches.This value to be set to atleast higher than the value specified in
	 * leaseRenewalIntervalInSeconds.
	 */
	private int leaseExpirationDurationInSeconds = 90;

	/**
	 * Gets the virtual host name defined for this instance.
	 *
	 * This is typically the way other instance would find this instance by using the
	 * virtual host name.Think of this as similar to the fully qualified domain name, that
	 * the users of your services will need to find this instance.
	 */
	@Value("${spring.application.name:unknown}")
	private String virtualHostName;

	/**
	 * Get the unique Id (within the scope of the appName) of this instance to be
	 * registered with eureka.
	 */
	private String instanceId;

	/**
	 * Gets the secure virtual host name defined for this instance.
	 *
	 * This is typically the way other instance would find this instance by using the
	 * secure virtual host name.Think of this as similar to the fully qualified domain
	 * name, that the users of your services will need to find this instance.
	 */
	private String secureVirtualHostName;

	/**
	 * Gets the AWS autoscaling group name associated with this instance. This information
	 * is specifically used in an AWS environment to automatically put an instance out of
	 * service after the instance is launched and it has been disabled for traffic..
	 */
	private String aSGName;

	/**
	 * Gets the metadata name/value pairs associated with this instance. This information
	 * is sent to eureka server and can be used by other instances.
	 */
	private Map<String, String> metadataMap = new HashMap<>();

	/**
	 * Returns the data center this instance is deployed. This information is used to get
	 * some AWS specific instance information if the instance is deployed in AWS.
	 */
	private DataCenterInfo dataCenterInfo = new MyDataCenterInfo(
			DataCenterInfo.Name.MyOwn);

	/**
	 * Get the IPAdress of the instance. This information is for academic purposes only as
	 * the communication from other instances primarily happen using the information
	 * supplied in {@link #getHostName(boolean)}.
	 */
	private String ipAddress;

	/**
	 * Gets the relative status page URL path for this instance. The status page URL is
	 * then constructed out of the hostName and the type of communication - secure or
	 * unsecure as specified in securePort and nonSecurePort.
	 *
	 * It is normally used for informational purposes for other services to find about the
	 * status of this instance. Users can provide a simple HTML indicating what is the
	 * current status of the instance.
	 */
	private String statusPageUrlPath = "/info";

	/**
	 * Gets the absolute status page URL path for this instance. The users can provide the
	 * statusPageUrlPath if the status page resides in the same instance talking to
	 * eureka, else in the cases where the instance is a proxy for some other server,
	 * users can provide the full URL. If the full URL is provided it takes precedence.
	 *
	 * It is normally used for informational purposes for other services to find about the
	 * status of this instance. Users can provide a simple HTML indicating what is the
	 * current status of the instance.
	 */
	private String statusPageUrl;

	/**
	 * Gets the relative home page URL Path for this instance. The home page URL is then
	 * constructed out of the hostName and the type of communication - secure or unsecure.
	 *
	 * It is normally used for informational purposes for other services to use it as a
	 * landing page.
	 */
	private String homePageUrlPath = "/";

	/**
	 * Gets the absolute home page URL for this instance. The users can provide the
	 * homePageUrlPath if the home page resides in the same instance talking to eureka,
	 * else in the cases where the instance is a proxy for some other server, users can
	 * provide the full URL. If the full URL is provided it takes precedence.
	 *
	 * It is normally used for informational purposes for other services to use it as a
	 * landing page. The full URL should follow the format http://${eureka.hostname}:7001/
	 * where the value ${eureka.hostname} is replaced at runtime.
	 */
	private String homePageUrl;

	/**
	 * Gets the relative health check URL path for this instance. The health check page
	 * URL is then constructed out of the hostname and the type of communication - secure
	 * or unsecure as specified in securePort and nonSecurePort.
	 *
	 * It is normally used for making educated decisions based on the health of the
	 * instance - for example, it can be used to determine whether to proceed deployments
	 * to an entire farm or stop the deployments without causing further damage.
	 */
	private String healthCheckUrlPath = "/health";

	/**
	 * Gets the absolute health check page URL for this instance. The users can provide
	 * the healthCheckUrlPath if the health check page resides in the same instance
	 * talking to eureka, else in the cases where the instance is a proxy for some other
	 * server, users can provide the full URL. If the full URL is provided it takes
	 * precedence.
	 *
	 * <p>
	 * It is normally used for making educated decisions based on the health of the
	 * instance - for example, it can be used to determine whether to proceed deployments
	 * to an entire farm or stop the deployments without causing further damage. The full
	 * URL should follow the format http://${eureka.hostname}:7001/ where the value
	 * ${eureka.hostname} is replaced at runtime.
	 */
	private String healthCheckUrl;

	/**
	 * Gets the absolute secure health check page URL for this instance. The users can
	 * provide the secureHealthCheckUrl if the health check page resides in the same
	 * instance talking to eureka, else in the cases where the instance is a proxy for
	 * some other server, users can provide the full URL. If the full URL is provided it
	 * takes precedence.
	 *
	 * <p>
	 * It is normally used for making educated decisions based on the health of the
	 * instance - for example, it can be used to determine whether to proceed deployments
	 * to an entire farm or stop the deployments without causing further damage. The full
	 * URL should follow the format http://${eureka.hostname}:7001/ where the value
	 * ${eureka.hostname} is replaced at runtime.
	 */
	private String secureHealthCheckUrl;

	/**
	 * Get the namespace used to find properties. Ignored in Spring Cloud.
	 */
	private String namespace = "eureka";

	/**
	 * The hostname if it can be determined at configuration time (otherwise it will be
	 * guessed from OS primitives).
	 */
	private String hostname;

	/**
	 * Flag to say that, when guessing a hostname, the IP address of the server should be
	 * used in prference to the hostname reported by the OS.
	 */
	private boolean preferIpAddress = false;

	/**
	 * Initial status to register with rmeote Eureka server.
	 */
	private InstanceStatus initialStatus = InstanceStatus.UP;

	private String[] defaultAddressResolutionOrder = new String[0];

	public String getHostname() {
		return getHostName(false);
	}

	@SuppressWarnings("unused")
	private EurekaInstanceConfigBean() {
	}

	public EurekaInstanceConfigBean(InetUtils inetUtils) {
		this.inetUtils = inetUtils;
		this.hostInfo = this.inetUtils.findFirstNonLoopbackHostInfo();
		this.ipAddress = this.hostInfo.getIpAddress();
		this.hostname = this.hostInfo.getHostname();
	}

	@Override
	public String getInstanceId() {
		if (this.instanceId == null && this.metadataMap != null) {
			return this.metadataMap.get("instanceId");
		}
		return this.instanceId;
	}

	@Override
	public boolean getSecurePortEnabled() {
		return this.securePortEnabled;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
		this.hostInfo.override = true;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
		this.hostInfo.override = true;
	}

	@Override
	public String getHostName(boolean refresh) {
		if (refresh && !this.hostInfo.override) {
			this.ipAddress = this.hostInfo.getIpAddress();
			this.hostname = this.hostInfo.getHostname();
		}
		return this.preferIpAddress ? this.ipAddress : this.hostname;
	}

	public String getAppname() {
		return this.appname;
	}

	public String getAppGroupName() {
		return this.appGroupName;
	}

	public boolean isInstanceEnabledOnit() {
		return this.instanceEnabledOnit;
	}

	public int getNonSecurePort() {
		return this.nonSecurePort;
	}

	public int getSecurePort() {
		return this.securePort;
	}

	public boolean isNonSecurePortEnabled() {
		return this.nonSecurePortEnabled;
	}

	public int getLeaseRenewalIntervalInSeconds() {
		return this.leaseRenewalIntervalInSeconds;
	}

	public int getLeaseExpirationDurationInSeconds() {
		return this.leaseExpirationDurationInSeconds;
	}

	public String getVirtualHostName() {
		return this.virtualHostName;
	}

	public String getSecureVirtualHostName() {
		return this.secureVirtualHostName;
	}

	public String getASGName() {
		return this.aSGName;
	}

	public Map<String, String> getMetadataMap() {
		return this.metadataMap;
	}

	public DataCenterInfo getDataCenterInfo() {
		return this.dataCenterInfo;
	}

	public String getIpAddress() {
		return this.ipAddress;
	}

	public String getStatusPageUrlPath() {
		return this.statusPageUrlPath;
	}

	public String getStatusPageUrl() {
		return this.statusPageUrl;
	}

	public String getHomePageUrlPath() {
		return this.homePageUrlPath;
	}

	public String getHomePageUrl() {
		return this.homePageUrl;
	}

	public String getHealthCheckUrlPath() {
		return this.healthCheckUrlPath;
	}

	public String getHealthCheckUrl() {
		return this.healthCheckUrl;
	}

	public String getSecureHealthCheckUrl() {
		return this.secureHealthCheckUrl;
	}

	public String getNamespace() {
		return this.namespace;
	}

	public boolean isPreferIpAddress() {
		return this.preferIpAddress;
	}

	public InstanceStatus getInitialStatus() {
		return this.initialStatus;
	}

	public String[] getDefaultAddressResolutionOrder() {
		return this.defaultAddressResolutionOrder;
	}

	public void setAppname(String appname) {
		this.appname = appname;
	}

	public void setAppGroupName(String appGroupName) {
		this.appGroupName = appGroupName;
	}

	public void setInstanceEnabledOnit(boolean instanceEnabledOnit) {
		this.instanceEnabledOnit = instanceEnabledOnit;
	}

	public void setNonSecurePort(int nonSecurePort) {
		this.nonSecurePort = nonSecurePort;
	}

	public void setSecurePort(int securePort) {
		this.securePort = securePort;
	}

	public void setNonSecurePortEnabled(boolean nonSecurePortEnabled) {
		this.nonSecurePortEnabled = nonSecurePortEnabled;
	}

	public void setSecurePortEnabled(boolean securePortEnabled) {
		this.securePortEnabled = securePortEnabled;
	}

	public void setLeaseRenewalIntervalInSeconds(int leaseRenewalIntervalInSeconds) {
		this.leaseRenewalIntervalInSeconds = leaseRenewalIntervalInSeconds;
	}

	public void setLeaseExpirationDurationInSeconds(
			int leaseExpirationDurationInSeconds) {
		this.leaseExpirationDurationInSeconds = leaseExpirationDurationInSeconds;
	}

	public void setVirtualHostName(String virtualHostName) {
		this.virtualHostName = virtualHostName;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

	public void setSecureVirtualHostName(String secureVirtualHostName) {
		this.secureVirtualHostName = secureVirtualHostName;
	}

	public void setASGName(String aSGName) {
		this.aSGName = aSGName;
	}

	public void setMetadataMap(Map<String, String> metadataMap) {
		this.metadataMap = metadataMap;
	}

	public void setDataCenterInfo(DataCenterInfo dataCenterInfo) {
		this.dataCenterInfo = dataCenterInfo;
	}

	public void setStatusPageUrlPath(String statusPageUrlPath) {
		this.statusPageUrlPath = statusPageUrlPath;
	}

	public void setStatusPageUrl(String statusPageUrl) {
		this.statusPageUrl = statusPageUrl;
	}

	public void setHomePageUrlPath(String homePageUrlPath) {
		this.homePageUrlPath = homePageUrlPath;
	}

	public void setHomePageUrl(String homePageUrl) {
		this.homePageUrl = homePageUrl;
	}

	public void setHealthCheckUrlPath(String healthCheckUrlPath) {
		this.healthCheckUrlPath = healthCheckUrlPath;
	}

	public void setHealthCheckUrl(String healthCheckUrl) {
		this.healthCheckUrl = healthCheckUrl;
	}

	public void setSecureHealthCheckUrl(String secureHealthCheckUrl) {
		this.secureHealthCheckUrl = secureHealthCheckUrl;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public void setPreferIpAddress(boolean preferIpAddress) {
		this.preferIpAddress = preferIpAddress;
	}

	public void setInitialStatus(InstanceStatus initialStatus) {
		this.initialStatus = initialStatus;
	}

	public void setDefaultAddressResolutionOrder(String[] defaultAddressResolutionOrder) {
		this.defaultAddressResolutionOrder = defaultAddressResolutionOrder;
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof EurekaInstanceConfigBean))
			return false;
		final EurekaInstanceConfigBean other = (EurekaInstanceConfigBean) o;
		if (!other.canEqual((Object) this))
			return false;
		final Object this$hostInfo = this.hostInfo;
		final Object other$hostInfo = other.hostInfo;
		if (this$hostInfo == null ?
				other$hostInfo != null :
				!this$hostInfo.equals(other$hostInfo))
			return false;
		final Object this$inetUtils = this.inetUtils;
		final Object other$inetUtils = other.inetUtils;
		if (this$inetUtils == null ?
				other$inetUtils != null :
				!this$inetUtils.equals(other$inetUtils))
			return false;
		final Object this$appname = this.appname;
		final Object other$appname = other.appname;
		if (this$appname == null ?
				other$appname != null :
				!this$appname.equals(other$appname))
			return false;
		final Object this$appGroupName = this.appGroupName;
		final Object other$appGroupName = other.appGroupName;
		if (this$appGroupName == null ?
				other$appGroupName != null :
				!this$appGroupName.equals(other$appGroupName))
			return false;
		if (this.instanceEnabledOnit != other.instanceEnabledOnit)
			return false;
		if (this.nonSecurePort != other.nonSecurePort)
			return false;
		if (this.securePort != other.securePort)
			return false;
		if (this.nonSecurePortEnabled != other.nonSecurePortEnabled)
			return false;
		if (this.securePortEnabled != other.securePortEnabled)
			return false;
		if (this.leaseRenewalIntervalInSeconds != other.leaseRenewalIntervalInSeconds)
			return false;
		if (this.leaseExpirationDurationInSeconds
				!= other.leaseExpirationDurationInSeconds)
			return false;
		final Object this$virtualHostName = this.virtualHostName;
		final Object other$virtualHostName = other.virtualHostName;
		if (this$virtualHostName == null ?
				other$virtualHostName != null :
				!this$virtualHostName.equals(other$virtualHostName))
			return false;
		final Object this$instanceId = this.getInstanceId();
		final Object other$instanceId = other.getInstanceId();
		if (this$instanceId == null ?
				other$instanceId != null :
				!this$instanceId.equals(other$instanceId))
			return false;
		final Object this$secureVirtualHostName = this.secureVirtualHostName;
		final Object other$secureVirtualHostName = other.secureVirtualHostName;
		if (this$secureVirtualHostName == null ?
				other$secureVirtualHostName != null :
				!this$secureVirtualHostName.equals(other$secureVirtualHostName))
			return false;
		final Object this$aSGName = this.aSGName;
		final Object other$aSGName = other.aSGName;
		if (this$aSGName == null ?
				other$aSGName != null :
				!this$aSGName.equals(other$aSGName))
			return false;
		final Object this$metadataMap = this.metadataMap;
		final Object other$metadataMap = other.metadataMap;
		if (this$metadataMap == null ?
				other$metadataMap != null :
				!this$metadataMap.equals(other$metadataMap))
			return false;
		final Object this$dataCenterInfo = this.dataCenterInfo;
		final Object other$dataCenterInfo = other.dataCenterInfo;
		if (this$dataCenterInfo == null ?
				other$dataCenterInfo != null :
				!this$dataCenterInfo.equals(other$dataCenterInfo))
			return false;
		final Object this$ipAddress = this.ipAddress;
		final Object other$ipAddress = other.ipAddress;
		if (this$ipAddress == null ?
				other$ipAddress != null :
				!this$ipAddress.equals(other$ipAddress))
			return false;
		final Object this$statusPageUrlPath = this.statusPageUrlPath;
		final Object other$statusPageUrlPath = other.statusPageUrlPath;
		if (this$statusPageUrlPath == null ?
				other$statusPageUrlPath != null :
				!this$statusPageUrlPath.equals(other$statusPageUrlPath))
			return false;
		final Object this$statusPageUrl = this.statusPageUrl;
		final Object other$statusPageUrl = other.statusPageUrl;
		if (this$statusPageUrl == null ?
				other$statusPageUrl != null :
				!this$statusPageUrl.equals(other$statusPageUrl))
			return false;
		final Object this$homePageUrlPath = this.homePageUrlPath;
		final Object other$homePageUrlPath = other.homePageUrlPath;
		if (this$homePageUrlPath == null ?
				other$homePageUrlPath != null :
				!this$homePageUrlPath.equals(other$homePageUrlPath))
			return false;
		final Object this$homePageUrl = this.homePageUrl;
		final Object other$homePageUrl = other.homePageUrl;
		if (this$homePageUrl == null ?
				other$homePageUrl != null :
				!this$homePageUrl.equals(other$homePageUrl))
			return false;
		final Object this$healthCheckUrlPath = this.healthCheckUrlPath;
		final Object other$healthCheckUrlPath = other.healthCheckUrlPath;
		if (this$healthCheckUrlPath == null ?
				other$healthCheckUrlPath != null :
				!this$healthCheckUrlPath.equals(other$healthCheckUrlPath))
			return false;
		final Object this$healthCheckUrl = this.healthCheckUrl;
		final Object other$healthCheckUrl = other.healthCheckUrl;
		if (this$healthCheckUrl == null ?
				other$healthCheckUrl != null :
				!this$healthCheckUrl.equals(other$healthCheckUrl))
			return false;
		final Object this$secureHealthCheckUrl = this.secureHealthCheckUrl;
		final Object other$secureHealthCheckUrl = other.secureHealthCheckUrl;
		if (this$secureHealthCheckUrl == null ?
				other$secureHealthCheckUrl != null :
				!this$secureHealthCheckUrl.equals(other$secureHealthCheckUrl))
			return false;
		final Object this$namespace = this.namespace;
		final Object other$namespace = other.namespace;
		if (this$namespace == null ?
				other$namespace != null :
				!this$namespace.equals(other$namespace))
			return false;
		final Object this$hostname = this.getHostname();
		final Object other$hostname = other.getHostname();
		if (this$hostname == null ?
				other$hostname != null :
				!this$hostname.equals(other$hostname))
			return false;
		if (this.preferIpAddress != other.preferIpAddress)
			return false;
		final Object this$initialStatus = this.initialStatus;
		final Object other$initialStatus = other.initialStatus;
		if (this$initialStatus == null ?
				other$initialStatus != null :
				!this$initialStatus.equals(other$initialStatus))
			return false;
		if (!java.util.Arrays.deepEquals(this.defaultAddressResolutionOrder,
				other.defaultAddressResolutionOrder))
			return false;
		return true;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $hostInfo = this.hostInfo;
		result = result * PRIME + ($hostInfo == null ? 0 : $hostInfo.hashCode());
		final Object $inetUtils = this.inetUtils;
		result = result * PRIME + ($inetUtils == null ? 0 : $inetUtils.hashCode());
		final Object $appname = this.appname;
		result = result * PRIME + ($appname == null ? 0 : $appname.hashCode());
		final Object $appGroupName = this.appGroupName;
		result = result * PRIME + ($appGroupName == null ? 0 : $appGroupName.hashCode());
		result = result * PRIME + (this.instanceEnabledOnit ? 79 : 97);
		result = result * PRIME + this.nonSecurePort;
		result = result * PRIME + this.securePort;
		result = result * PRIME + (this.nonSecurePortEnabled ? 79 : 97);
		result = result * PRIME + (this.securePortEnabled ? 79 : 97);
		result = result * PRIME + this.leaseRenewalIntervalInSeconds;
		result = result * PRIME + this.leaseExpirationDurationInSeconds;
		final Object $virtualHostName = this.virtualHostName;
		result = result * PRIME + ($virtualHostName == null ?
				0 :
				$virtualHostName.hashCode());
		final Object $instanceId = this.getInstanceId();
		result = result * PRIME + ($instanceId == null ? 0 : $instanceId.hashCode());
		final Object $secureVirtualHostName = this.secureVirtualHostName;
		result = result * PRIME + ($secureVirtualHostName == null ?
				0 :
				$secureVirtualHostName.hashCode());
		final Object $aSGName = this.aSGName;
		result = result * PRIME + ($aSGName == null ? 0 : $aSGName.hashCode());
		final Object $metadataMap = this.metadataMap;
		result = result * PRIME + ($metadataMap == null ? 0 : $metadataMap.hashCode());
		final Object $dataCenterInfo = this.dataCenterInfo;
		result = result * PRIME + ($dataCenterInfo == null ?
				0 :
				$dataCenterInfo.hashCode());
		final Object $ipAddress = this.ipAddress;
		result = result * PRIME + ($ipAddress == null ? 0 : $ipAddress.hashCode());
		final Object $statusPageUrlPath = this.statusPageUrlPath;
		result = result * PRIME + ($statusPageUrlPath == null ?
				0 :
				$statusPageUrlPath.hashCode());
		final Object $statusPageUrl = this.statusPageUrl;
		result =
				result * PRIME + ($statusPageUrl == null ? 0 : $statusPageUrl.hashCode());
		final Object $homePageUrlPath = this.homePageUrlPath;
		result = result * PRIME + ($homePageUrlPath == null ?
				0 :
				$homePageUrlPath.hashCode());
		final Object $homePageUrl = this.homePageUrl;
		result = result * PRIME + ($homePageUrl == null ? 0 : $homePageUrl.hashCode());
		final Object $healthCheckUrlPath = this.healthCheckUrlPath;
		result = result * PRIME + ($healthCheckUrlPath == null ?
				0 :
				$healthCheckUrlPath.hashCode());
		final Object $healthCheckUrl = this.healthCheckUrl;
		result = result * PRIME + ($healthCheckUrl == null ?
				0 :
				$healthCheckUrl.hashCode());
		final Object $secureHealthCheckUrl = this.secureHealthCheckUrl;
		result = result * PRIME + ($secureHealthCheckUrl == null ?
				0 :
				$secureHealthCheckUrl.hashCode());
		final Object $namespace = this.namespace;
		result = result * PRIME + ($namespace == null ? 0 : $namespace.hashCode());
		final Object $hostname = this.getHostname();
		result = result * PRIME + ($hostname == null ? 0 : $hostname.hashCode());
		result = result * PRIME + (this.preferIpAddress ? 79 : 97);
		final Object $initialStatus = this.initialStatus;
		result =
				result * PRIME + ($initialStatus == null ? 0 : $initialStatus.hashCode());
		result = result * PRIME + java.util.Arrays
				.deepHashCode(this.defaultAddressResolutionOrder);
		return result;
	}

	protected boolean canEqual(Object other) {
		return other instanceof EurekaInstanceConfigBean;
	}

	public String toString() {
		return "org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean(hostInfo="
				+ this.hostInfo + ", inetUtils=" + this.inetUtils + ", appname="
				+ this.appname + ", appGroupName=" + this.appGroupName
				+ ", instanceEnabledOnit=" + this.instanceEnabledOnit + ", nonSecurePort="
				+ this.nonSecurePort + ", securePort=" + this.securePort
				+ ", nonSecurePortEnabled=" + this.nonSecurePortEnabled
				+ ", securePortEnabled=" + this.securePortEnabled
				+ ", leaseRenewalIntervalInSeconds=" + this.leaseRenewalIntervalInSeconds
				+ ", leaseExpirationDurationInSeconds="
				+ this.leaseExpirationDurationInSeconds + ", virtualHostName="
				+ this.virtualHostName + ", instanceId=" + this.getInstanceId()
				+ ", secureVirtualHostName=" + this.secureVirtualHostName + ", aSGName="
				+ this.aSGName + ", metadataMap=" + this.metadataMap + ", dataCenterInfo="
				+ this.dataCenterInfo + ", ipAddress=" + this.ipAddress
				+ ", statusPageUrlPath=" + this.statusPageUrlPath + ", statusPageUrl="
				+ this.statusPageUrl + ", homePageUrlPath=" + this.homePageUrlPath
				+ ", homePageUrl=" + this.homePageUrl + ", healthCheckUrlPath="
				+ this.healthCheckUrlPath + ", healthCheckUrl=" + this.healthCheckUrl
				+ ", secureHealthCheckUrl=" + this.secureHealthCheckUrl + ", namespace="
				+ this.namespace + ", hostname=" + this.getHostname()
				+ ", preferIpAddress=" + this.preferIpAddress + ", initialStatus="
				+ this.initialStatus + ", defaultAddressResolutionOrder="
				+ java.util.Arrays.deepToString(this.defaultAddressResolutionOrder) + ")";
	}

	private HostInfo getHostInfo() {
		return this.hostInfo;
	}

	private InetUtils getInetUtils() {
		return this.inetUtils;
	}

	private void setHostInfo(HostInfo hostInfo) {
		this.hostInfo = hostInfo;
	}

	private void setInetUtils(InetUtils inetUtils) {
		this.inetUtils = inetUtils;
	}
}
