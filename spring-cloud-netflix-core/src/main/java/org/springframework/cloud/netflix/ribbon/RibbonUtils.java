package org.springframework.cloud.netflix.ribbon;

import java.net.URI;

import org.springframework.web.util.UriComponentsBuilder;

import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.IClientConfig;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.netflix.loadbalancer.Server;

/**
 * @author Spencer Gibb
 * @author Jacques-Etienne Beaudet
 */
public class RibbonUtils {

	public static final String VALUE_NOT_SET = "__not__set__";

	public static final String DEFAULT_NAMESPACE = "ribbon";

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
	 */
	public static URI updateToHttpsIfNeeded(URI uri, IClientConfig config, ServerIntrospector serverIntrospector,
			Server server) {
		String scheme = uri.getScheme();
		if (!"".equals(uri.toString()) && !"https".equals(scheme) && isSecure(config, serverIntrospector, server)) {
			UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUri(uri).scheme("https");
			if (uri.getRawQuery() != null) {
				// When building the URI, UriComponentsBuilder verify the allowed characters and does not 
				// support the '+' so we replace it for its equivalent '%20'.
				// See issue https://jira.spring.io/browse/SPR-10172
				uriComponentsBuilder.replaceQuery(uri.getRawQuery().replace("+", "%20"));
			}
			return uriComponentsBuilder.build(true).toUri();
		}
		return uri;
	}
}
