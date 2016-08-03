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

package org.springframework.cloud.netflix.zuul.filters.route.apache;

import java.io.InputStream;
import java.net.URI;

import org.springframework.cloud.netflix.ribbon.RibbonHttpResponse;
import org.springframework.cloud.netflix.ribbon.apache.RibbonApacheHttpRequest;
import org.springframework.cloud.netflix.ribbon.apache.RibbonApacheHttpResponse;
import org.springframework.cloud.netflix.ribbon.apache.RibbonLoadBalancingHttpClient;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommand;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.MultiValueMap;

import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.zuul.constants.ZuulConstants;
import com.netflix.zuul.context.RequestContext;
import org.springframework.util.StringUtils;

/**
 * @author Christian Lohmann
 */
public class HttpClientRibbonCommand extends HystrixCommand<ClientHttpResponse> implements
		RibbonCommand {

	private final RibbonLoadBalancingHttpClient client;
	private final String method;
	private final String uri;
	private final MultiValueMap<String, String> headers;
	private final MultiValueMap<String, String> params;
	private final InputStream requestEntity;
	private final Boolean retryable;

	public HttpClientRibbonCommand(final RibbonLoadBalancingHttpClient client,
			final String method, final String uri,
			final MultiValueMap<String, String> headers,
			final MultiValueMap<String, String> params, final InputStream requestEntity,
			final Boolean retryable) {
		this("default", client, method, uri, headers, params, requestEntity, retryable);
	}

	public HttpClientRibbonCommand(final String commandKey,
			final RibbonLoadBalancingHttpClient client, final String method,
			final String uri, final MultiValueMap<String, String> headers,
			final MultiValueMap<String, String> params, final InputStream requestEntity,
			final Boolean retryable) {
		super(getSetter(commandKey));
		this.client = client;
		this.method = method;
		this.uri = uri;
		this.headers = headers;
		this.params = params;
		this.requestEntity = requestEntity;
		this.retryable = retryable;
	}

	protected static Setter getSetter(final String commandKey) {

		// we want to default to semaphore-isolation since this wraps
		// 2 others commands that are already thread isolated
		final String name = ZuulConstants.ZUUL_EUREKA + commandKey
				+ ".semaphore.maxSemaphores";
		final DynamicIntProperty value = DynamicPropertyFactory.getInstance()
				.getIntProperty(name, 100);
		final HystrixCommandProperties.Setter setter = HystrixCommandProperties
				.Setter()
				.withExecutionIsolationStrategy(
						HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE)
				.withExecutionIsolationSemaphoreMaxConcurrentRequests(value.get());
		return Setter
				.withGroupKey(HystrixCommandGroupKey.Factory.asKey("RibbonCommand"))
				.andCommandKey(
						HystrixCommandKey.Factory.asKey(commandKey + "RibbonCommand"))
				.andCommandPropertiesDefaults(setter);
	}

	@Override
	protected ClientHttpResponse run() throws Exception {
		return forward();
	}

	protected ClientHttpResponse forward() throws Exception {
		final RequestContext context = RequestContext.getCurrentContext();
		Long contentLength = null;
		if (context.getRequest().getContentLength() != -1) {
			contentLength = new Long(context.getRequest().getContentLength());
		}
		URI uriInstance = new URI(this.uri);
		RibbonApacheHttpRequest request = new RibbonApacheHttpRequest(this.method,
				uriInstance, this.retryable, this.headers, this.params,
				this.requestEntity, contentLength);
		final RibbonApacheHttpResponse response = this.client
				.executeWithLoadBalancer(request);
		context.set("ribbonResponse", response);

		// Explicitly close the HttpResponse if the Hystrix command timed out to
		// release the underlying HTTP connection held by the response.
		//
		if (this.isResponseTimedOut()) {
			if (response != null) {
				response.close();
			}
		}

		return new RibbonHttpResponse(response);
	}

}
