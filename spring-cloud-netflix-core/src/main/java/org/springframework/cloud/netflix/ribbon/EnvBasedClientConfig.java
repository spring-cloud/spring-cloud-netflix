package org.springframework.cloud.netflix.ribbon;

import static com.netflix.client.config.CommonClientConfigKey.DeploymentContextBasedVipAddresses;

import java.util.concurrent.TimeUnit;

import com.netflix.client.config.CommonClientConfigKey;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.core.env.ConfigurableEnvironment;

import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfigKey;

/**
 * 
 * {@link com.netflix.client.config.IClientConfig} implementation that depends on Spring
 * environment for its values instead of using Archaius
 * 
 * @author Biju Kunjummen
 */
public class EnvBasedClientConfig extends DefaultClientConfigImpl {

	private RelaxedPropertyResolver propertyResolver;

	public static final IClientConfigKey<String> RETRYABLE_STATUS_CODES = new CommonClientConfigKey<String>("retryableStatusCodes") {};

	public EnvBasedClientConfig(ConfigurableEnvironment environment) {
		super();
		this.propertyResolver = new RelaxedPropertyResolver(environment);
	}
	
	@Override
	public void loadProperties(String restClientName) {
		setClientName(restClientName);
		loadDefaultValues();
	}

	@Override
	public void loadDefaultValues() {
		super.loadDefaultValues();
		putDefaultStringProperty(DeploymentContextBasedVipAddresses, this.getClientName());
		putDefaultStringProperty(RETRYABLE_STATUS_CODES, "");
	}

	@Override
	protected void putDefaultIntegerProperty(IClientConfigKey propName,
			Integer defaultValue) {
		putPropertyValue(propName, defaultValue, Integer.class);
	}

	@Override
	protected void putDefaultLongProperty(IClientConfigKey propName, Long defaultValue) {
		putPropertyValue(propName, defaultValue, Long.class);
	}

	@Override
	protected void putDefaultFloatProperty(IClientConfigKey propName,
			Float defaultValue) {
		putPropertyValue(propName, defaultValue, Float.class);
	}

	@Override
	protected void putDefaultStringProperty(IClientConfigKey propName,
			String defaultValue) {
		putPropertyValue(propName, defaultValue, String.class);
	}

	@Override
	protected void putDefaultBooleanProperty(IClientConfigKey propName,
			Boolean defaultValue) {
		putPropertyValue(propName, defaultValue, Boolean.class);
	}

	@Override
	protected void putDefaultTimeUnitProperty(IClientConfigKey propName, TimeUnit defaultValue) {
		putPropertyValue(propName, defaultValue, TimeUnit.class);
	}

	private <T> void putPropertyValue(IClientConfigKey propName, T defaultValue,
									  Class<T> clazz) {
		String propNameWithNamespace = getDefaultPropName(propName);
		T propValueForNamespace = this.propertyResolver.getProperty(propNameWithNamespace, clazz, defaultValue);
		String key = getInstancePropName(this.getClientName(), propName);
		T value = this.propertyResolver.getProperty(key, clazz, propValueForNamespace);
		setPropertyInternal(propName.key(), value);
	}

	@Override
	protected void setPropertyInternal(final String propName, Object value) {
		String stringValue = (value == null) ? "" : String.valueOf(value);
		properties.put(propName, stringValue);
	}

	@Override
	protected Object getProperty(String key) {
		return properties.get(key);
	}
	

}
