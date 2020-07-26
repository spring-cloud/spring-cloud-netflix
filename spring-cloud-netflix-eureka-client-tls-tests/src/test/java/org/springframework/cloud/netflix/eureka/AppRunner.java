/*
 * Copyright 2018-2019 the original author or authors.
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

package org.springframework.cloud.netflix.eureka;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.SocketUtils;

public class AppRunner implements AutoCloseable {

	private Class<?> appClass;

	private Map<String, String> props;

	private ConfigurableApplicationContext app;

	public AppRunner(Class<?> appClass) {
		this.appClass = appClass;
		props = new LinkedHashMap<>();
	}

	public void property(String key, String value) {
		props.put(key, value);
	}

	public void start() {
		if (app == null) {
			SpringApplicationBuilder builder = new SpringApplicationBuilder(appClass);
			builder.properties("spring.jmx.enabled=false");
			builder.properties(String.format("server.port=%d", availabeTcpPort()));
			builder.properties(props());

			app = builder.build().run();
		}
	}

	private int availabeTcpPort() {
		return SocketUtils.findAvailableTcpPort();
	}

	private String[] props() {
		List<String> result = new ArrayList<>();

		for (String key : props.keySet()) {
			String value = props.get(key);
			result.add(String.format("%s=%s", key, value));
		}

		return result.toArray(new String[0]);
	}

	public void stop() {
		if (app != null) {
			app.stop();
			app = null;
		}
	}

	public ConfigurableApplicationContext app() {
		return app;
	}

	public String getProperty(String key) {
		return app.getEnvironment().getProperty(key);
	}

	public <T> T getBean(Class<T> type) {
		return app.getBean(type);
	}

	public ApplicationContext parent() {
		return app.getParent();
	}

	public <T> Map<String, T> getParentBeans(Class<T> type) {
		return parent().getBeansOfType(type);
	}

	public int port() {
		if (app == null) {
			throw new RuntimeException("App is not running.");
		}
		return app.getEnvironment().getProperty("server.port", Integer.class, -1);
	}

	public String root() {
		if (app == null) {
			throw new RuntimeException("App is not running.");
		}

		String protocol = tlsEnabled() ? "https" : "http";
		return String.format("%s://localhost:%d/", protocol, port());
	}

	private boolean tlsEnabled() {
		return app.getEnvironment().getProperty("server.ssl.enabled", Boolean.class,
				false);
	}

	@Override
	public void close() {
		stop();
	}

}
