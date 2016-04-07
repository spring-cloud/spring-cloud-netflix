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

import org.springframework.cloud.netflix.ribbon.RibbonHttpResponse;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.MultiValueMap;

import com.netflix.client.http.HttpRequest;
import com.netflix.client.http.HttpRequest.Builder;
import com.netflix.client.http.HttpRequest.Verb;
import com.netflix.client.http.HttpResponse;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.niws.client.http.RestClient;
import com.netflix.zuul.context.RequestContext;

import static org.springframework.cloud.netflix.zuul.filters.route.RibbonCommand.Utils.getSetter;

/**
 * Hystrix wrapper around Eureka Ribbon command
 *
 * see original
 * https://github.com/Netflix/zuul/blob/master/zuul-netflix/src/main/java/com/
 * netflix/zuul/dependency/ribbon/hystrix/RibbonCommand.java
 */
@SuppressWarnings("deprecation")
public class RestClientRibbonCommand extends HystrixCommand<ClientHttpResponse> implements RibbonCommand {

	private RestClient restClient;

	private Verb verb;

	private URI uri;

	private Boolean retryable;

	private MultiValueMap<String, String> headers;

	private MultiValueMap<String, String> params;

	private InputStream requestEntity;

	public RestClientRibbonCommand(RestClient restClient, Verb verb, String uri,
								   Boolean retryable,
								   MultiValueMap<String, String> headers,
								   MultiValueMap<String, String> params, InputStream requestEntity,
								   ZuulProperties properties)
			throws URISyntaxException {
		this("default", restClient, verb, uri, retryable , headers, params, requestEntity, properties);
	}

	public RestClientRibbonCommand(String commandKey, RestClient restClient, Verb verb, String uri,
								   Boolean retryable,
								   MultiValueMap<String, String> headers,
								   MultiValueMap<String, String> params, InputStream requestEntity,
								   ZuulProperties properties)
			throws URISyntaxException {
		super(getSetter(commandKey, properties));
		this.restClient = restClient;
		this.verb = verb;
		this.uri = new URI(uri);
		this.retryable = retryable;
		this.headers = headers;
		this.params = params;
		this.requestEntity = requestEntity;
	}

	@Override
	protected ClientHttpResponse run() throws Exception {
		return forward();
	}

	protected ClientHttpResponse forward() throws Exception {
		RequestContext context = RequestContext.getCurrentContext();
		Builder builder = HttpRequest.newBuilder().verb(this.verb).uri(this.uri)
				.entity(this.requestEntity);

		if(this.retryable != null) {
			builder.setRetriable(this.retryable);
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

		customizeRequest(builder);

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

		RibbonHttpResponse ribbonHttpResponse = new RibbonHttpResponse(response);

		return ribbonHttpResponse;
	}

	protected void customizeRequest(Builder requestBuilder) {

	}

	protected MultiValueMap<String, String> getHeaders() {
		return this.headers;
	}

	protected MultiValueMap<String, String> getParams() {
		return this.params;
	}

	protected InputStream getRequestEntity() {
		return this.requestEntity;
	}

	protected RestClient getRestClient() {
		return this.restClient;
	}

	protected Boolean getRetryable() {
		return this.retryable;
	}

	protected URI getUri() {
		return this.uri;
	}

	protected Verb getVerb() {
		return this.verb;
	}
}
