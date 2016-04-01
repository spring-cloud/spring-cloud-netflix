package org.springframework.cloud.netflix.ribbon;

import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;

/**
 * @author Spencer Gibb
 */
public class RibbonProperyUtils {

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

}
