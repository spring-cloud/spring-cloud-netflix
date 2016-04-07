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
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommand;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.MultiValueMap;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.zuul.context.RequestContext;

import static org.springframework.cloud.netflix.zuul.filters.route.RibbonCommand.Utils.getSetter;

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

	public HttpClientRibbonCommand(RibbonLoadBalancingHttpClient client,
								   String method, String uri,
								   MultiValueMap<String, String> headers,
								   MultiValueMap<String, String> params, InputStream requestEntity,
								   Boolean retryable, ZuulProperties properties) {
		this("default", client, method, uri, headers, params, requestEntity, retryable, properties);
	}

	public HttpClientRibbonCommand(String commandKey,
			RibbonLoadBalancingHttpClient client, String method,
			String uri, MultiValueMap<String, String> headers,
			MultiValueMap<String, String> params, InputStream requestEntity,
			Boolean retryable, ZuulProperties properties) {
		super(getSetter(commandKey, properties));
		this.client = client;
		this.method = method;
		this.uri = uri;
		this.headers = headers;
		this.params = params;
		this.requestEntity = requestEntity;
		this.retryable = retryable;
	}

	@Override
	protected ClientHttpResponse run() throws Exception {
		return forward();
	}

	protected ClientHttpResponse forward() throws Exception {
		final RequestContext context = RequestContext.getCurrentContext();
		URI uriInstance = new URI(this.uri);
		RibbonApacheHttpRequest request = new RibbonApacheHttpRequest(this.method,
				uriInstance, this.retryable, this.headers, this.params,
				this.requestEntity);
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
