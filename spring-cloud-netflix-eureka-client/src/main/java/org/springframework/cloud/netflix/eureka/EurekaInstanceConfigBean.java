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

import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtils.HostInfo;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.appinfo.MyDataCenterInfo;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Dave Syer
 * @author Spencer Gibb
 * @author Ryan Baxter
 */
@Data
@ConfigurationProperties("eureka.instance")
public class EurekaInstanceConfigBean implements CloudEurekaInstanceConfig, EnvironmentAware {

	private static final String UNKNOWN = "unknown";

	@Getter(AccessLevel.PRIVATE)
	@Setter(AccessLevel.PRIVATE)
	private HostInfo hostInfo;

	@Getter(AccessLevel.PRIVATE)
	@Setter(AccessLevel.PRIVATE)
	private InetUtils inetUtils;

	/**
	 * Get the name of the application to be registered with eureka.
	 */
	private String appname = UNKNOWN;

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
	private String virtualHostName = UNKNOWN;

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
	private String secureVirtualHostName = UNKNOWN;

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
	private Environment environment;

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

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
		// set some defaults from the environment, but allow the defaults to use relaxed binding
		RelaxedPropertyResolver springPropertyResolver = new RelaxedPropertyResolver(this.environment, "spring.application.");
		String springAppName = springPropertyResolver.getProperty("name");
		if(StringUtils.hasText(springAppName)) {
			setAppname(springAppName);
			setVirtualHostName(springAppName);
			setSecureVirtualHostName(springAppName);
		}
	}
}
