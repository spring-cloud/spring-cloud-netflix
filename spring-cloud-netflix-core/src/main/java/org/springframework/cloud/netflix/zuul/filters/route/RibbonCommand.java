package org.springframework.cloud.netflix.zuul.filters.route;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

import com.netflix.client.http.HttpRequest;
import com.netflix.client.http.HttpRequest.Builder;
import com.netflix.client.http.HttpRequest.Verb;
import com.netflix.client.http.HttpResponse;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
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
public class RibbonCommand extends HystrixCommand<HttpResponse> {

	private RestClient restClient;
	private Verb verb;
	private URI uri;
	private MultivaluedMap<String, String> headers;
	private MultivaluedMap<String, String> params;
	private InputStream requestEntity;

	public RibbonCommand(RestClient restClient, Verb verb, String uri,
			MultivaluedMap<String, String> headers,
			MultivaluedMap<String, String> params, InputStream requestEntity)
			throws URISyntaxException {
		this("default", restClient, verb, uri, headers, params, requestEntity);
	}

	public RibbonCommand(String commandKey, RestClient restClient, Verb verb, String uri,
			MultivaluedMap<String, String> headers,
			MultivaluedMap<String, String> params, InputStream requestEntity)
			throws URISyntaxException {

		super(
				Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(commandKey))
						.andCommandPropertiesDefaults(
						// we want to default to semaphore-isolation since this wraps
						// 2 others commands that are already thread isolated
								HystrixCommandProperties
										.Setter()
										.withExecutionIsolationStrategy(
												HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE)
										.withExecutionIsolationSemaphoreMaxConcurrentRequests(
												DynamicPropertyFactory
														.getInstance()
														.getIntProperty(
																ZuulConstants.ZUUL_EUREKA
																		+ commandKey
																		+ ".semaphore.maxSemaphores",
																100).get())));

		this.restClient = restClient;
		this.verb = verb;
		this.uri = new URI(uri);
		this.headers = headers;
		this.params = params;
		this.requestEntity = requestEntity;
	}

	@Override
	protected HttpResponse run() throws Exception {
		try {
			return forward();
		}
		catch (Exception e) {
			throw e;
		}
	}

	private HttpResponse forward() throws Exception {

		RequestContext context = RequestContext.getCurrentContext();

		Builder builder = HttpRequest.newBuilder().verb(this.verb).uri(this.uri)
				.entity(this.requestEntity);

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
		return response;
	}

}