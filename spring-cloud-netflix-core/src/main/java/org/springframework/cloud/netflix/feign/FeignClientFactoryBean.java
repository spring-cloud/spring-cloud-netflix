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

package org.springframework.cloud.netflix.feign;

import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import feign.Client;
import feign.Contract;
import feign.Feign;
import feign.Logger;
import feign.Request;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.Target.HardCodedTarget;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import feign.slf4j.Slf4jLogger;

/**
 * @author Spencer Gibb
 */
class FeignClientFactoryBean implements FactoryBean<Object>, InitializingBean,
		ApplicationContextAware {

	private static final Targeter targeter;

	static {
		Targeter targeterToUse;
		if (ClassUtils.isPresent("feign.hystrix.HystrixFeign",
				FeignClientFactoryBean.class.getClassLoader())) {
			targeterToUse = new HystrixTargeter();
		}
		else {
			targeterToUse = new DefaultTargeter();
		}
		targeter = targeterToUse;
	}

	private Class<?> type;

	private String name;

	private String url;

	private boolean decode404;

	private ApplicationContext applicationContext;

	private Class<?> fallback = void.class;

	public FeignClientFactoryBean() {
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.hasText(this.name, "Name must be set");
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.applicationContext = context;
	}

	protected Feign.Builder feign(FeignContext context) {
		Logger logger = getOptional(context, Logger.class);

		if (logger == null) {
			logger = new Slf4jLogger(this.type);
		}

		// @formatter:off
		Feign.Builder builder = get(context, Feign.Builder.class)
				// required values
				.logger(logger)
				.encoder(get(context, Encoder.class))
				.decoder(get(context, Decoder.class))
				.contract(get(context, Contract.class));
		// @formatter:on

		// optional values
		Logger.Level level = getOptional(context, Logger.Level.class);
		if (level != null) {
			builder.logLevel(level);
		}
		Retryer retryer = getOptional(context, Retryer.class);
		if (retryer != null) {
			builder.retryer(retryer);
		}
		ErrorDecoder errorDecoder = getOptional(context, ErrorDecoder.class);
		if (errorDecoder != null) {
			builder.errorDecoder(errorDecoder);
		}
		Request.Options options = getOptional(context, Request.Options.class);
		if (options != null) {
			builder.options(options);
		}
		Map<String, RequestInterceptor> requestInterceptors = context.getInstances(
				this.name, RequestInterceptor.class);
		if (requestInterceptors != null) {
			builder.requestInterceptors(requestInterceptors.values());
		}

		if (decode404) {
			builder.decode404();
		}

		return builder;
	}

	protected <T> T get(FeignContext context, Class<T> type) {
		T instance = context.getInstance(this.name, type);
		if (instance == null) {
			throw new IllegalStateException("No bean found of type " + type + " for "
					+ this.name);
		}
		return instance;
	}

	protected <T> T getOptional(FeignContext context, Class<T> type) {
		return context.getInstance(this.name, type);
	}

	protected <T> T loadBalance(Feign.Builder builder, FeignContext context,
			HardCodedTarget<T> target) {
		Client client = getOptional(context, Client.class);
		if (client != null) {
			builder.client(client);
			return targeter.target(this, builder, context, target);
		}

		throw new IllegalStateException(
				"No Feign Client for loadBalancing defined. Did you forget to include spring-cloud-starter-ribbon?");
	}

	@Override
	public Object getObject() throws Exception {
		FeignContext context = applicationContext.getBean(FeignContext.class);
		Feign.Builder builder = feign(context);

		if (!StringUtils.hasText(this.url)) {
			String url;
			if (!this.name.startsWith("http")) {
				url = "http://" + this.name;
			}
			else {
				url = this.name;
			}
			return loadBalance(builder, context, new HardCodedTarget<>(this.type,
					this.name, url));
		}
		if (StringUtils.hasText(this.url) && !this.url.startsWith("http")) {
			this.url = "http://" + this.url;
		}
		return targeter.target(this, builder, context, new HardCodedTarget<>(
				this.type, this.name, this.url));
	}

	@Override
	public Class<?> getObjectType() {
		return this.type;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	public Class<?> getType() {
		return this.type;
	}

	public String getName() {
		return this.name;
	}

	public String getUrl() {
		return this.url;
	}

	public boolean isDecode404() {
		return this.decode404;
	}

	public ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

	public Class<?> getFallback() {
		return this.fallback;
	}

	public void setType(Class<?> type) {
		this.type = type;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setDecode404(boolean decode404) {
		this.decode404 = decode404;
	}

	public void setFallback(Class<?> fallback) {
		this.fallback = fallback;
	}

	public String toString() {
		return "org.springframework.cloud.netflix.feign.FeignClientFactoryBean(type="
				+ this.type + ", name=" + this.name + ", url=" + this.url + ", decode404="
				+ this.decode404 + ", applicationContext=" + this.applicationContext
				+ ", fallback=" + this.fallback + ")";
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof FeignClientFactoryBean))
			return false;
		final FeignClientFactoryBean other = (FeignClientFactoryBean) o;
		if (!other.canEqual((Object) this))
			return false;
		final Object this$type = this.getType();
		final Object other$type = other.getType();
		if (this$type == null ? other$type != null : !this$type.equals(other$type))
			return false;
		final Object this$name = this.getName();
		final Object other$name = other.getName();
		if (this$name == null ? other$name != null : !this$name.equals(other$name))
			return false;
		final Object this$url = this.getUrl();
		final Object other$url = other.getUrl();
		if (this$url == null ? other$url != null : !this$url.equals(other$url))
			return false;
		if (this.isDecode404() != other.isDecode404())
			return false;
		final Object this$applicationContext = this.getApplicationContext();
		final Object other$applicationContext = other.getApplicationContext();
		if (this$applicationContext == null ?
				other$applicationContext != null :
				!this$applicationContext.equals(other$applicationContext))
			return false;
		final Object this$fallback = this.getFallback();
		final Object other$fallback = other.getFallback();
		if (this$fallback == null ?
				other$fallback != null :
				!this$fallback.equals(other$fallback))
			return false;
		return true;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $type = this.getType();
		result = result * PRIME + ($type == null ? 0 : $type.hashCode());
		final Object $name = this.getName();
		result = result * PRIME + ($name == null ? 0 : $name.hashCode());
		final Object $url = this.getUrl();
		result = result * PRIME + ($url == null ? 0 : $url.hashCode());
		result = result * PRIME + (this.isDecode404() ? 79 : 97);
		final Object $applicationContext = this.getApplicationContext();
		result = result * PRIME + ($applicationContext == null ?
				0 :
				$applicationContext.hashCode());
		final Object $fallback = this.getFallback();
		result = result * PRIME + ($fallback == null ? 0 : $fallback.hashCode());
		return result;
	}

	protected boolean canEqual(Object other) {
		return other instanceof FeignClientFactoryBean;
	}

	interface Targeter {
		<T> T target(FeignClientFactoryBean factory, Feign.Builder feign, FeignContext context,
				HardCodedTarget<T> target);
	}

	static class DefaultTargeter implements Targeter {

		@Override
		public <T> T target(FeignClientFactoryBean factory, Feign.Builder feign, FeignContext context,
							HardCodedTarget<T> target) {
			return feign.target(target);
		}
	}

	@SuppressWarnings("unchecked")
	static class HystrixTargeter implements Targeter {

		@Override
		public <T> T target(FeignClientFactoryBean factory, Feign.Builder feign, FeignContext context,
							HardCodedTarget<T> target) {
			if (factory.fallback == void.class
					|| !(feign instanceof feign.hystrix.HystrixFeign.Builder)) {
				return feign.target(target);
			}

			Object fallbackInstance = context.getInstance(factory.name, factory.fallback);
			if (fallbackInstance == null) {
				throw new IllegalStateException(String.format(
						"No fallback instance of type %s found for feign client %s",
						factory.fallback, factory.name));
			}

			if (!target.type().isAssignableFrom(factory.fallback)) {
				throw new IllegalStateException(
						String.format(
								"Incompatible fallback instance. Fallback of type %s is not assignable to %s for feign client %s",
								factory.fallback, target.type(), factory.name));
			}

			feign.hystrix.HystrixFeign.Builder builder = (feign.hystrix.HystrixFeign.Builder) feign;
			return builder.target(target, (T) fallbackInstance);
		}
	}

}
