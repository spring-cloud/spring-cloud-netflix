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

package org.springframework.cloud.netflix.zuul.filters.route;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import com.netflix.client.http.HttpRequest;
import com.netflix.client.http.HttpRequest.Builder;
import com.netflix.client.http.HttpRequest.Verb;
import com.netflix.client.http.HttpResponse;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy;
import com.netflix.niws.client.http.RestClient;
import com.netflix.zuul.constants.ZuulConstants;
import com.netflix.zuul.context.RequestContext;

/**
 * Hystrix wrapper around Eureka Ribbon command
 *
 * see original
 * https://github.com/Netflix/zuul/blob/master/zuul-netflix/src/main/java/com/
 * netflix/zuul/dependency/ribbon/hystrix/RibbonCommand.java
 */
@SuppressWarnings("deprecation")
public class RibbonCommand extends HystrixCommand<HttpResponse> {

	private RestClient restClient;

	private Verb verb;

	private URI uri;
	
	private Boolean retryable;

	private MultiValueMap<String, String> headers;

	private MultiValueMap<String, String> params;

	private InputStream requestEntity;

	public RibbonCommand(RestClient restClient, Verb verb, String uri,
			Boolean retryable,
			MultiValueMap<String, String> headers,
            MultiValueMap<String, String> params, InputStream requestEntity)
			throws URISyntaxException {
		this("default", restClient, verb, uri, retryable , headers, params, requestEntity);
	}

	public RibbonCommand(String commandKey, RestClient restClient, Verb verb, String uri,
			Boolean retryable,
            MultiValueMap<String, String> headers,
            MultiValueMap<String, String> params, InputStream requestEntity)
			throws URISyntaxException {
		super(getSetter(commandKey));
		this.restClient = restClient;
		this.verb = verb;
		this.uri = (StringUtils.hasText(uri))? UriComponentsBuilder.fromUriString(uri).build().toUri() : new URI(uri);
		this.retryable = retryable;
		this.headers = headers;
		this.params = params;
		this.requestEntity = requestEntity;
	}

	private static HystrixCommand.Setter getSetter(String commandKey) {
		// we want to default to semaphore-isolation since this wraps
		// 2 others commands that are already thread isolated
		String name = ZuulConstants.ZUUL_EUREKA + commandKey + ".semaphore.maxSemaphores";
		DynamicIntProperty value = DynamicPropertyFactory.getInstance().getIntProperty(
				name, 100);
		HystrixCommandProperties.Setter setter = HystrixCommandProperties.Setter()
				.withExecutionIsolationStrategy(ExecutionIsolationStrategy.SEMAPHORE)
				.withExecutionIsolationSemaphoreMaxConcurrentRequests(value.get());
		return Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("RibbonCommand"))
				.andCommandKey(HystrixCommandKey.Factory.asKey(commandKey + "RibbonCommand"))
				.andCommandPropertiesDefaults(setter);
	}

	@Override
	protected HttpResponse run() throws Exception {
		return forward();
	}

	private HttpResponse forward() throws Exception {
		RequestContext context = RequestContext.getCurrentContext();
		Builder builder = HttpRequest.newBuilder().verb(this.verb).uri(this.uri)
				.entity(this.requestEntity);
		
		if(retryable != null) {
			builder.setRetriable(retryable);
		}
		
		for (String name : this.headers.keySet()) {
			List<String> values = this.headers.get(name);
			for (String value : values) {
				builder.header(name, value);
			}
		}
		for (String name : this.params.keySet()) {
			List<String> values = this.params.get(name);
			for (String value : values) {
				builder.queryParams(name, value);
			}
		}
		HttpRequest httpClientRequest = builder.build();
		HttpResponse response = this.restClient
				.executeWithLoadBalancer(httpClientRequest);
		context.set("ribbonResponse", response);
		
		// Explicitly close the HttpResponse if the Hystrix command timed out to
		// release the underlying HTTP connection held by the response.
		// 
		if( this.isResponseTimedOut() ) {	
			if( response!= null ) {
				response.close();
			}
		}
		return response;
	}

}
