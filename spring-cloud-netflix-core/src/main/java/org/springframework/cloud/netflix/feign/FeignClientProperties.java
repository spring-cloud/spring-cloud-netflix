/*
 * Copyright 2013-2017 the original author or authors.
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
package org.springframework.cloud.netflix.feign;

import feign.Logger;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Eko Kurniawan Khannedy
 */
@ConfigurationProperties("feign.client")
public class FeignClientProperties {

	private boolean defaultToProperties = true;

	private String defaultConfig = "default";

	private Map<String, FeignClientConfiguration> config = new HashMap<>();

	public boolean isDefaultToProperties() {
		return defaultToProperties;
	}

	public void setDefaultToProperties(boolean defaultToProperties) {
		this.defaultToProperties = defaultToProperties;
	}

	public String getDefaultConfig() {
		return defaultConfig;
	}

	public void setDefaultConfig(String defaultConfig) {
		this.defaultConfig = defaultConfig;
	}

	public Map<String, FeignClientConfiguration> getConfig() {
		return config;
	}

	public void setConfig(Map<String, FeignClientConfiguration> config) {
		this.config = config;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		FeignClientProperties that = (FeignClientProperties) o;
		return defaultToProperties == that.defaultToProperties &&
				Objects.equals(defaultConfig, that.defaultConfig) &&
				Objects.equals(config, that.config);
	}

	@Override
	public int hashCode() {
		return Objects.hash(defaultToProperties, defaultConfig, config);
	}

	public static class FeignClientConfiguration {

		private Logger.Level loggerLevel;

		private Integer connectTimeout;

		private Integer readTimeout;

		private Class<Retryer> retryer;

		private Class<ErrorDecoder> errorDecoder;

		private List<Class<RequestInterceptor>> requestInterceptors;

		private Boolean decode404;

		public Logger.Level getLoggerLevel() {
			return loggerLevel;
		}

		public void setLoggerLevel(Logger.Level loggerLevel) {
			this.loggerLevel = loggerLevel;
		}

		public Integer getConnectTimeout() {
			return connectTimeout;
		}

		public void setConnectTimeout(Integer connectTimeout) {
			this.connectTimeout = connectTimeout;
		}

		public Integer getReadTimeout() {
			return readTimeout;
		}

		public void setReadTimeout(Integer readTimeout) {
			this.readTimeout = readTimeout;
		}

		public Class<Retryer> getRetryer() {
			return retryer;
		}

		public void setRetryer(Class<Retryer> retryer) {
			this.retryer = retryer;
		}

		public Class<ErrorDecoder> getErrorDecoder() {
			return errorDecoder;
		}

		public void setErrorDecoder(Class<ErrorDecoder> errorDecoder) {
			this.errorDecoder = errorDecoder;
		}

		public List<Class<RequestInterceptor>> getRequestInterceptors() {
			return requestInterceptors;
		}

		public void setRequestInterceptors(List<Class<RequestInterceptor>> requestInterceptors) {
			this.requestInterceptors = requestInterceptors;
		}

		public Boolean getDecode404() {
			return decode404;
		}

		public void setDecode404(Boolean decode404) {
			this.decode404 = decode404;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			FeignClientConfiguration that = (FeignClientConfiguration) o;
			return loggerLevel == that.loggerLevel &&
					Objects.equals(connectTimeout, that.connectTimeout) &&
					Objects.equals(readTimeout, that.readTimeout) &&
					Objects.equals(retryer, that.retryer) &&
					Objects.equals(errorDecoder, that.errorDecoder) &&
					Objects.equals(requestInterceptors, that.requestInterceptors) &&
					Objects.equals(decode404, that.decode404);
		}

		@Override
		public int hashCode() {
			return Objects.hash(loggerLevel, connectTimeout, readTimeout, retryer,
					errorDecoder, requestInterceptors, decode404);
		}
	}

}
