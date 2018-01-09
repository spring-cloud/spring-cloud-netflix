package org.springframework.cloud.netflix.ribbon;

import java.net.URI;

import com.netflix.client.config.IClientConfigKey;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.IClientConfig;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.netflix.loadbalancer.Server;

import java.util.HashMap;
import java.util.Map;

import static com.netflix.client.config.CommonClientConfigKey.DeploymentContextBasedVipAddresses;
import static com.netflix.client.config.CommonClientConfigKey.EnableZoneAffinity;
import static com.netflix.client.config.CommonClientConfigKey.Port;
import static com.netflix.client.config.CommonClientConfigKey.SecurePort;
import static com.netflix.client.config.DefaultClientConfigImpl.DEFAULT_CONNECT_TIMEOUT;
import static com.netflix.client.config.DefaultClientConfigImpl.DEFAULT_FOLLOW_REDIRECTS;
import static com.netflix.client.config.DefaultClientConfigImpl.DEFAULT_MAX_TOTAL_CONNECTIONS;
import static com.netflix.client.config.DefaultClientConfigImpl.DEFAULT_OK_TO_RETRY_ON_ALL_OPERATIONS;
import static com.netflix.client.config.DefaultClientConfigImpl.DEFAULT_PORT;
import static com.netflix.client.config.DefaultClientConfigImpl.DEFAULT_READ_TIMEOUT;

/**
 * @author Spencer Gibb
 * @author Jacques-Etienne Beaudet
 * @author Tim Ysewyn
 */
public class RibbonUtils {

	public static final String VALUE_NOT_SET = "__not__set__";
	public static final String DEFAULT_NAMESPACE = "ribbon";

	private static final Map<String, String> unsecureSchemeMapping;
	static
	{
		unsecureSchemeMapping = new HashMap<>();
		unsecureSchemeMapping.put("http", "https");
		unsecureSchemeMapping.put("ws", "wss");
	}

	public static void initializeRibbonDefaults(String serviceId) {
		setRibbonProperty(serviceId, DeploymentContextBasedVipAddresses.key(),
				serviceId);
		setRibbonProperty(serviceId, EnableZoneAffinity.key(), "true");
	}

	public static RibbonProperties from(IClientConfig config) {
		return new RibbonProperties(config);
	}

	//TODO: add more commonly used properties
	//TODO: refactor s-c-netflix to use this class where possible
	public static class RibbonProperties {
		private final IClientConfig config;

		RibbonProperties(IClientConfig config) {
			this.config = config;
		}

		public Integer getPort() {
			return get(Port);
		}

		public int port() {
			return get(Port, DEFAULT_PORT);
		}

		public Integer getSecurePort() {
			return this.config.get(SecurePort);
		}

		public Boolean getSecure() {
			return get(CommonClientConfigKey.IsSecure);
		}

		public boolean isSecure() {
			return get(CommonClientConfigKey.IsSecure, false);
		}

		public Integer getReadTimeout() {
			return get(CommonClientConfigKey.ReadTimeout);
		}

		public int readTimeout() {
			return get(CommonClientConfigKey.ReadTimeout, DEFAULT_READ_TIMEOUT);
		}

		public Integer getConnectTimeout() {
			return get(CommonClientConfigKey.ConnectTimeout);
		}

		public int connectTimeout() {
			return get(CommonClientConfigKey.ConnectTimeout, DEFAULT_CONNECT_TIMEOUT);
		}

		public Boolean getOkToRetryOnAllOperations() {
			return get(CommonClientConfigKey.OkToRetryOnAllOperations);
		}

		public boolean isOkToRetryOnAllOperations() {
			return get(CommonClientConfigKey.OkToRetryOnAllOperations,
					DEFAULT_OK_TO_RETRY_ON_ALL_OPERATIONS);
		}

		public Boolean getFollowRedirects() {
			return get(CommonClientConfigKey.FollowRedirects);
		}

		public boolean isFollowRedirects() {
			return get(CommonClientConfigKey.FollowRedirects,
					DEFAULT_FOLLOW_REDIRECTS);
		}

		public Integer getMaxTotalConnections() {
			return get(CommonClientConfigKey.MaxTotalConnections);
		}

		public int maxTotalConnections() {
			return get(CommonClientConfigKey.MaxTotalConnections, DEFAULT_MAX_TOTAL_CONNECTIONS);
		}

		public <T> boolean has(IClientConfigKey<T> key) {
			return this.config.containsProperty(key);
		}

		public <T> T get(IClientConfigKey<T> key) {
			return this.config.get(key);
		}

		public <T> T get(IClientConfigKey<T> key, T defaultValue) {
			return this.config.get(key, defaultValue);
		}
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
	 * Determine if client is secure. If the supplied {@link IClientConfig} has the {@link CommonClientConfigKey#IsSecure}
	 * set, return that value. Otherwise, query the supplied {@link ServerIntrospector}.
	 * @param config the supplied client configuration.
	 * @param serverIntrospector
	 * @param server
	 * @return true if the client is secure
	 */
	public static boolean isSecure(IClientConfig config, ServerIntrospector serverIntrospector, Server server) {
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
	 * {@link #isSecure(IClientConfig, ServerIntrospector, Server)} is true, update the scheme.
	 * This assumes the uri is already encoded to avoid double encoding.
	 *
	 * @param uri
	 * @param config
	 * @param serverIntrospector
	 * @param server
	 * @return
	 *
	 * @deprecated use {@link #updateToSecureConnectionIfNeeded}
	 */
	public static URI updateToHttpsIfNeeded(URI uri, IClientConfig config, ServerIntrospector serverIntrospector,
			Server server) {
		return updateToSecureConnectionIfNeeded(uri, config, serverIntrospector, server);
	}

	/**
	 * Replace the scheme to the secure variant if needed. If the {@link #unsecureSchemeMapping} map contains the uri
	 * scheme and {@link #isSecure(IClientConfig, ServerIntrospector, Server)} is true, update the scheme.
	 * This assumes the uri is already encoded to avoid double encoding.
	 *
	 * @param uri
	 * @param config
	 * @param serverIntrospector
	 * @param server
	 * @return
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
		UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUri(uri).scheme(scheme);
		if (uri.getRawQuery() != null) {
			// When building the URI, UriComponentsBuilder verify the allowed characters and does not
			// support the '+' so we replace it for its equivalent '%20'.
			// See issue https://jira.spring.io/browse/SPR-10172
			uriComponentsBuilder.replaceQuery(uri.getRawQuery().replace("+", "%20"));
		}
		return uriComponentsBuilder.build(true).toUri();
	}
}
