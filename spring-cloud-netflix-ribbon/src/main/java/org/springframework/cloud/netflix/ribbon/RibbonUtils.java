/*
 * Copyright 2016-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.ribbon;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.IClientConfig;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.netflix.loadbalancer.Server;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import static com.netflix.client.config.CommonClientConfigKey.DeploymentContextBasedVipAddresses;
import static com.netflix.client.config.CommonClientConfigKey.EnableZoneAffinity;

/**
 * @author Spencer Gibb
 * @author Jacques-Etienne Beaudet
 * @author Tim Ysewyn
 */
public final class RibbonUtils {

	/**
	 * Used to verify if property value is set.
	 */
	public static final String VALUE_NOT_SET = "__not__set__";

	/**
	 * Default Ribbon namespace.
	 */
	public static final String DEFAULT_NAMESPACE = "ribbon";

	private static final Map<String, String> unsecureSchemeMapping;

	static {
		unsecureSchemeMapping = new HashMap<>();
		unsecureSchemeMapping.put("http", "https");
		unsecureSchemeMapping.put("ws", "wss");
	}

	private RibbonUtils() {
		throw new AssertionError("Must not instantiate utility class.");
	}

	public static void initializeRibbonDefaults(String serviceId) {
		setRibbonProperty(serviceId, DeploymentContextBasedVipAddresses.key(), serviceId);
		setRibbonProperty(serviceId, EnableZoneAffinity.key(), "true");
	}

	public static void setRibbonProperty(String serviceId, String suffix, String value) {
		// how to set the namespace properly?
		String key = getRibbonKey(serviceId, suffix);
		DynamicStringProperty property = getProperty(key);
		if (property.get().equals(VALUE_NOT_SET)) {
			ConfigurationManager.getConfigInstance().setProperty(key, value);
		}
	}

	public static String getRibbonKey(String serviceId, String suffix) {
		return serviceId + "." + DEFAULT_NAMESPACE + "." + suffix;
	}

	public static DynamicStringProperty getProperty(String key) {
		return DynamicPropertyFactory.getInstance().getStringProperty(key, VALUE_NOT_SET);
	}

	/**
	 * Determine if client is secure. If the supplied {@link IClientConfig} has the
	 * {@link CommonClientConfigKey#IsSecure} set, return that value. Otherwise, query the
	 * supplied {@link ServerIntrospector}.
	 * @param config the supplied client configuration.
	 * @param serverIntrospector used to verify if the server provides secure connections
	 * @param server to verify
	 * @return true if the client is secure
	 */
	public static boolean isSecure(IClientConfig config,
			ServerIntrospector serverIntrospector, Server server) {
		if (config != null) {
			Boolean isSecure = config.get(CommonClientConfigKey.IsSecure);
			if (isSecure != null) {
				return isSecure;
			}
		}

		return serverIntrospector.isSecure(server);
	}

	/**
	 * Replace the scheme to https if needed. If the uri doesn't start with https and
	 * {@link #isSecure(IClientConfig, ServerIntrospector, Server)} is true, update the
	 * scheme. This assumes the uri is already encoded to avoid double encoding.
	 * @param uri to modify if required
	 * @param config Ribbon {@link IClientConfig} configuration
	 * @param serverIntrospector used to verify if the server provides secure connections
	 * @param server to verify
	 * @return {@link URI} updated to https if necessary
	 * @deprecated use {@link #updateToSecureConnectionIfNeeded}
	 */
	public static URI updateToHttpsIfNeeded(URI uri, IClientConfig config,
			ServerIntrospector serverIntrospector, Server server) {
		return updateToSecureConnectionIfNeeded(uri, config, serverIntrospector, server);
	}

	/**
	 * Replace the scheme to the secure variant if needed. If the
	 * {@link #unsecureSchemeMapping} map contains the uri scheme and
	 * {@link #isSecure(IClientConfig, ServerIntrospector, Server)} is true, update the
	 * scheme. This assumes the uri is already encoded to avoid double encoding.
	 * @param uri to modify if required
	 * @param ribbonServer to verify if it provides secure connections
	 * @return {@link URI} updated if required
	 */
	static URI updateToSecureConnectionIfNeeded(URI uri, ServiceInstance ribbonServer) {
		String scheme = uri.getScheme();

		if (StringUtils.isEmpty(scheme)) {
			scheme = "http";
		}

		if (!StringUtils.isEmpty(uri.toString())
				&& unsecureSchemeMapping.containsKey(scheme) && ribbonServer.isSecure()) {
			return upgradeConnection(uri, unsecureSchemeMapping.get(scheme));
		}
		return uri;
	}

	/**
	 * Replace the scheme to the secure variant if needed. If the
	 * {@link #unsecureSchemeMapping} map contains the uri scheme and
	 * {@link #isSecure(IClientConfig, ServerIntrospector, Server)} is true, update the
	 * scheme. This assumes the uri is already encoded to avoid double encoding.
	 * @param uri to modify if required
	 * @param config the supplied client configuration
	 * @param serverIntrospector used to verify if the server provides secure connections
	 * @param server to verify
	 * @return {@link URI} updated if required
	 */
	public static URI updateToSecureConnectionIfNeeded(URI uri, IClientConfig config,
			ServerIntrospector serverIntrospector, Server server) {
		String scheme = uri.getScheme();

		if (StringUtils.isEmpty(scheme)) {
			scheme = "http";
		}

		if (!StringUtils.isEmpty(uri.toString())
				&& unsecureSchemeMapping.containsKey(scheme)
				&& isSecure(config, serverIntrospector, server)) {
			return upgradeConnection(uri, unsecureSchemeMapping.get(scheme));
		}
		return uri;
	}

	private static URI upgradeConnection(URI uri, String scheme) {
		UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUri(uri)
				.scheme(scheme);
		if (uri.getRawQuery() != null) {
			// When building the URI, UriComponentsBuilder verify the allowed characters
			// and does not
			// support the '+' so we replace it for its equivalent '%20'.
			// See issue https://jira.spring.io/browse/SPR-10172
			uriComponentsBuilder.replaceQuery(uri.getRawQuery().replace("+", "%20"));
		}
		return uriComponentsBuilder.build(true).toUri();
	}

}
