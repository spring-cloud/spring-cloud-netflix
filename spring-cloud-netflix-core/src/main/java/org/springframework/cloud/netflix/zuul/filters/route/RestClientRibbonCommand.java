/*
 * Copyright 2013-2016 the original author or authors.
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
 *
 */

package org.springframework.cloud.netflix.zuul.filters.route;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.route.support.AbstractRibbonCommand;
import org.springframework.http.HttpMethod;
import org.springframework.util.MultiValueMap;
import com.netflix.client.config.IClientConfig;
import com.netflix.client.http.HttpRequest;
import com.netflix.client.http.HttpResponse;
import com.netflix.niws.client.http.RestClient;

import static org.springframework.cloud.netflix.ribbon.support.RibbonRequestCustomizer.Runner.customize;

/**
 * Hystrix wrapper around Eureka Ribbon command
 *
 * see <a href="https://github.com/Netflix/zuul/blob/master/zuul-netflix/src/main/java/com/netflix/zuul/dependency/ribbon/hystrix/RibbonCommand.java">original</a>
 */
@SuppressWarnings("deprecation")
public class RestClientRibbonCommand extends AbstractRibbonCommand<RestClient, HttpRequest, HttpResponse> {

	public RestClientRibbonCommand(String commandKey, RestClient client,
			RibbonCommandContext context, ZuulProperties zuulProperties) {
		super(commandKey, client, context, zuulProperties);
	}

	public RestClientRibbonCommand(String commandKey, RestClient client,
								   RibbonCommandContext context, ZuulProperties zuulProperties,
								   ZuulFallbackProvider zuulFallbackProvider) {
		super(commandKey, client, context, zuulProperties, zuulFallbackProvider);
	}

	public RestClientRibbonCommand(String commandKey, RestClient client,
								   RibbonCommandContext context, ZuulProperties zuulProperties,
								   ZuulFallbackProvider zuulFallbackProvider, IClientConfig config) {
		super(commandKey, client, context, zuulProperties, zuulFallbackProvider, config);
	}

	@Deprecated
	public RestClientRibbonCommand(String commandKey, RestClient restClient,
			HttpRequest.Verb verb, String uri, Boolean retryable,
			MultiValueMap<String, String> headers, MultiValueMap<String, String> params,
			InputStream requestEntity) {
		this(commandKey, restClient, new RibbonCommandContext(commandKey, verb.verb(),
				uri, retryable, headers, params, requestEntity), new ZuulProperties());
	}

	@Override
	protected HttpRequest createRequest() throws Exception {
		final InputStream requestEntity;
		// ApacheHttpClient4Handler does not support body in delete requests
		if (getContext().getMethod().equalsIgnoreCase(HttpMethod.DELETE.toString())) {
			requestEntity = null;
		} else {
			requestEntity = this.context.getRequestEntity();
		}

		HttpRequest.Builder builder = HttpRequest.newBuilder()
				.verb(getVerb(this.context.getMethod())).uri(this.context.uri())
				.entity(requestEntity);

		if (this.context.getRetryable() != null) {
			builder.setRetriable(this.context.getRetryable());
		}

		for (String name : this.context.getHeaders().keySet()) {
			List<String> values = this.context.getHeaders().get(name);
			for (String value : values) {
				builder.header(name, value);
			}
		}
		for (String name : this.context.getParams().keySet()) {
			List<String> values = this.context.getParams().get(name);
			for (String value : values) {
				builder.queryParams(name, value);
			}
		}

		customizeRequest(builder);

		return builder.build();
	}

	protected void customizeRequest(HttpRequest.Builder requestBuilder) {
		customize(this.context.getRequestCustomizers(), requestBuilder);
	}

	@Deprecated
	public URI getUri() {
		return this.context.uri();
	}

	@SuppressWarnings("unused")
	@Deprecated
	public HttpRequest.Verb getVerb() {
		return getVerb(this.context.getVerb());
	}

	protected static HttpRequest.Verb getVerb(String method) {
		if (method == null)
			return HttpRequest.Verb.GET;
		try {
			return HttpRequest.Verb.valueOf(method.toUpperCase());
		}
		catch (IllegalArgumentException e) {
			return HttpRequest.Verb.GET;
		}
	}

}
