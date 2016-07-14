/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.cloud.netflix.metrics;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.util.ClassUtils;

/**
 * @author Dave Syer
 */
public class ServoEnvironmentPostProcessor implements EnvironmentPostProcessor {

	private static final Log log = LogFactory.getLog(ServoEnvironmentPostProcessor.class);

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment,
			SpringApplication application) {
		if (ClassUtils.isPresent("com.netflix.servo.monitor.Monitors", null)) {
			// Make spring AOP default to target class so RestTemplates can be customized
			log.debug("Setting 'spring.aop.proxyTargetClass=true' to make spring AOP default to target class so RestTemplates can be customized");
			addDefaultProperty(environment, "spring.aop.proxyTargetClass", "true");
		}
	}

	private void addDefaultProperty(ConfigurableEnvironment environment, String name,
			String value) {
		MutablePropertySources sources = environment.getPropertySources();
		Map<String, Object> map = null;
		if (sources.contains("defaultProperties")) {
			PropertySource<?> source = sources.get("defaultProperties");
			if (source instanceof MapPropertySource) {
				map = ((MapPropertySource) source).getSource();
			}
		}
		else {
			map = new LinkedHashMap<>();
			sources.addLast(new MapPropertySource("defaultProperties", map));
		}
		if (map != null) {
			map.put(name, value);
		}
	}

}
