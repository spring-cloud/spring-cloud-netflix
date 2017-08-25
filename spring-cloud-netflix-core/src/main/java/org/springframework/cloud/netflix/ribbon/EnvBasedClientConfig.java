package org.springframework.cloud.netflix.ribbon;

import static com.netflix.client.config.CommonClientConfigKey.DeploymentContextBasedVipAddresses;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.core.env.ConfigurableEnvironment;

import com.netflix.client.VipAddressResolver;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.IClientConfig;
import com.netflix.client.config.IClientConfigKey;

/**
 * 
 * {@link com.netflix.client.config.IClientConfig} implementation that depends on Spring
 * environment for its values instead of using Archaius
 * 
 * @author Biju Kunjummen
 */
public class EnvBasedClientConfig implements IClientConfig {

	private RelaxedPropertyResolver propertyResolver;

	private String clientName;

	private String nameSpace;

	private Map<String, Object> properties = new HashMap<>();

	private volatile VipAddressResolver resolver = null;
	

	public static final IClientConfigKey<String> RETRYABLE_STATUS_CODES = new CommonClientConfigKey<String>(
			"retryableStatusCodes") {
	};

	public EnvBasedClientConfig(ConfigurableEnvironment environment) {
		super();
		this.propertyResolver = new RelaxedPropertyResolver(environment);
		this.nameSpace = RibbonClientConfigDefaults.DEFAULT_PROPERTY_NAME_SPACE;
	}

	@Override
	public String getClientName() {
		return this.clientName;
	}

	public void setClientName(String clientName) {
		this.clientName = clientName;
	}

	@Override
	public String getNameSpace() {
		return this.nameSpace;
	}

	@Override
	public void loadProperties(String restClientName) {
		setClientName(restClientName);
		loadDefaultValues();
	}

	@Override
	public void loadDefaultValues() {
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.MaxHttpConnectionsPerHost.key(),
				RibbonClientConfigDefaults.DEFAULT_MAX_HTTP_CONNECTIONS_PER_HOST, Integer.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.MaxTotalHttpConnections.key(),
				RibbonClientConfigDefaults.DEFAULT_MAX_TOTAL_HTTP_CONNECTIONS, Integer.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.EnableConnectionPool.key(),
				RibbonClientConfigDefaults.DEFAULT_ENABLE_CONNECTION_POOL, Boolean.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.MaxConnectionsPerHost.key(),
				RibbonClientConfigDefaults.DEFAULT_MAX_CONNECTIONS_PER_HOST, Integer.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.MaxTotalConnections.key(),
				RibbonClientConfigDefaults.DEFAULT_MAX_TOTAL_CONNECTIONS, Integer.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.ConnectTimeout.key(),
				RibbonClientConfigDefaults.DEFAULT_CONNECT_TIMEOUT, Integer.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.ConnectionManagerTimeout.key(),
				RibbonClientConfigDefaults.DEFAULT_CONNECTION_MANAGER_TIMEOUT, Integer.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.ReadTimeout.key(),
				RibbonClientConfigDefaults.DEFAULT_READ_TIMEOUT, Integer.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.MaxAutoRetries.key(),
				RibbonClientConfigDefaults.DEFAULT_MAX_AUTO_RETRIES, Integer.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.MaxAutoRetriesNextServer.key(),
				RibbonClientConfigDefaults.DEFAULT_MAX_AUTO_RETRIES_NEXT_SERVER, Integer.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.OkToRetryOnAllOperations.key(),
				RibbonClientConfigDefaults.DEFAULT_OK_TO_RETRY_ON_ALL_OPERATIONS, Boolean.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.FollowRedirects.key(),
				RibbonClientConfigDefaults.DEFAULT_FOLLOW_REDIRECTS, Boolean.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.ConnectionPoolCleanerTaskEnabled.key(),
				RibbonClientConfigDefaults.DEFAULT_CONNECTION_POOL_CLEANER_TASK_ENABLED, Boolean.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.ConnIdleEvictTimeMilliSeconds.key(),
				RibbonClientConfigDefaults.DEFAULT_CONNECTIONIDLE_TIME_IN_MSECS, Integer.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.ConnectionCleanerRepeatInterval.key(),
				RibbonClientConfigDefaults.DEFAULT_CONNECTION_IDLE_TIMERTASK_REPEAT_IN_MSECS, Integer.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.EnableGZIPContentEncodingFilter.key(),
				RibbonClientConfigDefaults.DEFAULT_ENABLE_GZIP_CONTENT_ENCODING_FILTER, Boolean.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.ProxyHost.key(), null, String.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.ProxyPort.key(), null, String.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.Port.key(), RibbonClientConfigDefaults.DEFAULT_PORT, Integer.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.EnablePrimeConnections.key(),
				RibbonClientConfigDefaults.DEFAULT_ENABLE_PRIME_CONNECTIONS, Boolean.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.MaxRetriesPerServerPrimeConnection.key(),
				RibbonClientConfigDefaults.DEFAULT_MAX_RETRIES_PER_SERVER_PRIME_CONNECTION, Integer.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.MaxTotalTimeToPrimeConnections.key(),
				RibbonClientConfigDefaults.DEFAULT_MAX_TOTAL_TIME_TO_PRIME_CONNECTIONS, Integer.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.PrimeConnectionsURI.key(),
				RibbonClientConfigDefaults.DEFAULT_PRIME_CONNECTIONS_URI, String.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.PoolMinThreads.key(),
				RibbonClientConfigDefaults.DEFAULT_POOL_MIN_THREADS, Integer.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.PoolMaxThreads.key(),
				RibbonClientConfigDefaults.DEFAULT_POOL_MAX_THREADS, Integer.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.PoolKeepAliveTime.key(),
				RibbonClientConfigDefaults.DEFAULT_POOL_KEEP_ALIVE_TIME, Long.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.PoolKeepAliveTimeUnits.key(),
				RibbonClientConfigDefaults.DEFAULT_POOL_KEEP_ALIVE_TIME_UNITS, TimeUnit.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.EnableZoneAffinity.key(),
				RibbonClientConfigDefaults.DEFAULT_ENABLE_ZONE_AFFINITY, Boolean.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.EnableZoneExclusivity.key(),
				RibbonClientConfigDefaults.DEFAULT_ENABLE_ZONE_EXCLUSIVITY, Boolean.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.ClientClassName.key(),
				RibbonClientConfigDefaults.DEFAULT_CLIENT_CLASSNAME, String.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.NFLoadBalancerClassName.key(),
				RibbonClientConfigDefaults.DEFAULT_NFLOADBALANCER_CLASSNAME, String.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.NFLoadBalancerRuleClassName.key(),
				RibbonClientConfigDefaults.DEFAULT_NFLOADBALANCER_RULE_CLASSNAME, String.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.NFLoadBalancerPingClassName.key(),
				RibbonClientConfigDefaults.DEFAULT_NFLOADBALANCER_PING_CLASSNAME, String.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.PrioritizeVipAddressBasedServers.key(),
				RibbonClientConfigDefaults.DEFAULT_PRIORITIZE_VIP_ADDRESS_BASED_SERVERS, Boolean.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.MinPrimeConnectionsRatio.key(),
				RibbonClientConfigDefaults.DEFAULT_MIN_PRIME_CONNECTIONS_RATIO, Float.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.PrimeConnectionsClassName.key(),
				RibbonClientConfigDefaults.DEFAULT_PRIME_CONNECTIONS_CLASS, String.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.NIWSServerListClassName.key(),
				RibbonClientConfigDefaults.DEFAULT_SEVER_LIST_CLASS, String.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.VipAddressResolverClassName.key(),
				RibbonClientConfigDefaults.DEFAULT_VIPADDRESS_RESOLVER_CLASSNAME, String.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.IsClientAuthRequired.key(),
				RibbonClientConfigDefaults.DEFAULT_IS_CLIENT_AUTH_REQUIRED, Boolean.class);
		// putDefaultStringProperty(CommonClientConfigKey.RequestIdHeaderName,
		// getDefaultRequestIdHeaderName());
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.UseIPAddrForServer.key(),
				RibbonClientConfigDefaults.DEFAULT_USEIPADDRESS_FOR_SERVER, Boolean.class);
		setPropertyWithPrecedenceAndDefaults(CommonClientConfigKey.ListOfServers.key(), "", String.class);

		setPropertyWithPrecedenceAndDefaults(DeploymentContextBasedVipAddresses.key(), this.getClientName(), String.class);
		setPropertyWithPrecedenceAndDefaults(RETRYABLE_STATUS_CODES.key(), "", String.class);
	}

	@Override
	public Map<String, Object> getProperties() {
		return Collections.unmodifiableMap(this.properties);
	}

	@Override
	public void setProperty(IClientConfigKey key, Object value) {
		setPropertyInternal(key.key(), value);
	}

	@Override
	public Object getProperty(IClientConfigKey key) {
		return getProperty(key.key());
	}

	@Override
	public Object getProperty(IClientConfigKey key, Object defaultVal) {
		Object value = getProperty(key);
		if (value != null) {
			return value;
		}
		return defaultVal;
	}

	@Override
	public boolean containsProperty(IClientConfigKey key) {
		return getProperty(key) != null;
	}

	@Override
	public String resolveDeploymentContextbasedVipAddresses() {
		String deploymentContextBasedVipAddressesMacro = (String) getProperty(CommonClientConfigKey.DeploymentContextBasedVipAddresses);
		if (deploymentContextBasedVipAddressesMacro == null) {
			return null;
		}
		return getVipAddressResolver().resolve(deploymentContextBasedVipAddressesMacro, this);
	}

	@Override
	public int getPropertyAsInteger(IClientConfigKey key, int defaultValue) {
		Object rawValue = getProperty(key);
		if (rawValue != null) {
			try {
				return Integer.parseInt(String.valueOf(rawValue));
			}
			catch (NumberFormatException e) {
				return defaultValue;
			}
		}
		return defaultValue;
	}

	@Override
	public String getPropertyAsString(IClientConfigKey key, String defaultValue) {
		Object value = this.getProperty(key);
		if (value != null) {
			return String.valueOf(value);
		}
		return defaultValue;
	}

	@Override
	public boolean getPropertyAsBoolean(IClientConfigKey key, boolean defaultValue) {
		Object rawValue = getProperty(key);
		if (rawValue != null) {
			try {
				return Boolean.valueOf(String.valueOf(rawValue));
			}
			catch (NumberFormatException e) {
				return defaultValue;
			}
		}
		return defaultValue;
	}

	@Override
	public <T> T get(IClientConfigKey<T> key) {
		Object obj = getProperty(key.key());
		if (obj == null) {
			return null;
		}
		Class<T> type = key.type();
		if (type.isInstance(obj)) {
			return type.cast(obj);
		}
		else {
			if (obj instanceof String) {
				String stringValue = (String) obj;
				if (Integer.class.equals(type)) {
					return (T) Integer.valueOf(stringValue);
				}
				else if (Boolean.class.equals(type)) {
					return (T) Boolean.valueOf(stringValue);
				}
				else if (Float.class.equals(type)) {
					return (T) Float.valueOf(stringValue);
				}
				else if (Long.class.equals(type)) {
					return (T) Long.valueOf(stringValue);
				}
				else if (Double.class.equals(type)) {
					return (T) Double.valueOf(stringValue);
				}
				else if (TimeUnit.class.equals(type)) {
					return (T) TimeUnit.valueOf(stringValue);
				}
				throw new IllegalArgumentException(
						"Unable to convert string value to desired type " + type);
			}

			throw new IllegalArgumentException(
					"Unable to convert value to desired type " + type);
		}
	}

	@Override
	public <T> T get(IClientConfigKey<T> key, T defaultValue) {
		T value = get(key);
		if (value == null) {
			value = defaultValue;
		}
		return value;
	}

	@Override
	public <T> IClientConfig set(IClientConfigKey<T> key, T value) {
		properties.put(key.key(), value);
		return this;
	}

	public void setNameSpace(String nameSpace) {
		this.nameSpace = nameSpace;
	}

	/**
	 * Set the value for the property in the following precedence order:
	 * 1. Check clientname.namespace.key
	 * 2. Then namespace.key
	 * 3. Then the provided default value
	 * 
	 * @param propName Name of the property
	 * @param defaultValue default value for the property
	 * @param clazz type of value
	 */
	private <T> void setPropertyWithPrecedenceAndDefaults(String propName, T defaultValue, Class<T> clazz) {
		String propNameWithNamespace = getDefaultPropName(propName);
		T propValueForNamespace = this.propertyResolver.getProperty(propNameWithNamespace,
				clazz, defaultValue);
		String key = getInstancePropName(this.getClientName(), propName);
		T value = this.propertyResolver.getProperty(key, clazz, propValueForNamespace);
		setPropertyInternal(propName, value);
	}

	private void setPropertyInternal(final String propName, Object value) {
		String stringValue = (value == null) ? null : String.valueOf(value);
		properties.put(propName, stringValue);
	}

	private Object getProperty(String key) {
		return properties.get(key);
	}

	private String getDefaultPropName(String propName) {
		return getNameSpace() + "." + propName;
	}

	private String getInstancePropName(String restClientName, String key) {
		return restClientName + "." + getNameSpace() + "." + key;
	}

	private VipAddressResolver getVipAddressResolver() {
		if (resolver == null) {
			synchronized (this) {
				if (resolver == null) {
					try {
						resolver = (VipAddressResolver) Class
								.forName((String) getProperty(CommonClientConfigKey.VipAddressResolverClassName))
								.newInstance();
					} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
						throw new RuntimeException("Cannot instantiate VipAddressResolver", e);
					}
				}
			}
		}
		return resolver;
	}

}
