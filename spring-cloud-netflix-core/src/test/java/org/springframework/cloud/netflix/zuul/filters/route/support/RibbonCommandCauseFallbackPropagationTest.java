/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.cloud.netflix.zuul.filters.route.support;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import com.netflix.client.AbstractLoadBalancerAwareClient;
import com.netflix.client.ClientException;
import com.netflix.client.ClientRequest;
import com.netflix.client.IResponse;
import com.netflix.client.RequestSpecificRetryHandler;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.client.http.HttpResponse;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.exception.HystrixTimeoutException;

import org.junit.Test;

import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.route.FallbackProvider;
import org.springframework.cloud.netflix.zuul.filters.route.ZuulFallbackProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dominik Mostek
 */
public class RibbonCommandCauseFallbackPropagationTest {

	@Test
	public void providerIsCalledInCaseOfException() throws Exception {
		TestZuulFallbackProviderWithoutCause provider = new TestZuulFallbackProviderWithoutCause(
				HttpStatus.INTERNAL_SERVER_ERROR);
		RuntimeException exception = new RuntimeException("Failed!");
		TestRibbonCommand testCommand = new TestRibbonCommand(new TestClient(exception),
				provider);

		ClientHttpResponse response = testCommand.execute();

		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@Test
	public void causeIsProvidedForNewInterface() throws Exception {
		TestFallbackProvider provider = TestFallbackProvider
				.withResponse(HttpStatus.NOT_FOUND);
		RuntimeException exception = new RuntimeException("Failed!");
		TestRibbonCommand testCommand = new TestRibbonCommand(new TestClient(exception),
				provider);

		ClientHttpResponse response = testCommand.execute();

		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		Throwable cause = provider.getCause();
		assertThat(cause.getClass()).isEqualTo(exception.getClass());
		assertThat(cause.getMessage()).isEqualTo(exception.getMessage());
	}

	@Test
	public void executionExceptionIsUsedInsteadWhenFailedExceptionIsNull()
			throws Exception {
		TestFallbackProvider provider = TestFallbackProvider
				.withResponse(HttpStatus.BAD_GATEWAY);
		final RuntimeException exception = new RuntimeException("Failed!");
		TestRibbonCommand testCommand = new TestRibbonCommand(new TestClient(exception),
				provider) {
			@Override
			public Throwable getFailedExecutionException() {
				return null;
			}

			@Override
			public Throwable getExecutionException() {
				return exception;
			}
		};

		ClientHttpResponse response = testCommand.execute();

		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
	}

	@Test
	public void timeoutExceptionIsPropagated() throws Exception {
		TestFallbackProvider provider = TestFallbackProvider
				.withResponse(HttpStatus.CONFLICT);
		RuntimeException exception = new RuntimeException("Failed!");
		TestRibbonCommand testCommand = new TestRibbonCommand(new TestClient(exception),
				provider, 1) {
			@Override
			protected ClientRequest createRequest() throws Exception {
				Thread.sleep(5);
				return super.createRequest();
			}
		};

		ClientHttpResponse response = testCommand.execute();

		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(provider.getCause()).isNotNull();
		assertThat(provider.getCause().getClass())
				.isEqualTo(HystrixTimeoutException.class);
	}

	public static class TestRibbonCommand extends
			AbstractRibbonCommand<AbstractLoadBalancerAwareClient<ClientRequest, HttpResponse>, ClientRequest, HttpResponse> {

		public TestRibbonCommand(
				AbstractLoadBalancerAwareClient<ClientRequest, HttpResponse> client,
				ZuulFallbackProvider fallbackProvider) {
			this(client, new ZuulProperties(), fallbackProvider);
		}

		public TestRibbonCommand(
				AbstractLoadBalancerAwareClient<ClientRequest, HttpResponse> client,
				ZuulProperties zuulProperties, ZuulFallbackProvider fallbackProvider) {
			super("testCommand" + UUID.randomUUID(), client, null, zuulProperties,
					fallbackProvider);
		}

		public TestRibbonCommand(
				AbstractLoadBalancerAwareClient<ClientRequest, HttpResponse> client,
				ZuulFallbackProvider fallbackProvider, int timeout) {
			// different name is used because of properties caching
			super(getSetter("testCommand" + UUID.randomUUID(), new ZuulProperties(), new DefaultClientConfigImpl())
					.andCommandPropertiesDefaults(defauts(timeout)), client, null,
					fallbackProvider, null);
		}

		private static HystrixCommandProperties.Setter defauts(final int timeout) {
			return HystrixCommandProperties.Setter().withExecutionTimeoutEnabled(true)
					.withExecutionIsolationStrategy(
							HystrixCommandProperties.ExecutionIsolationStrategy.THREAD)
					.withExecutionTimeoutInMilliseconds(timeout);
		}

		@Override
		protected ClientRequest createRequest() throws Exception {
			return null;
		}
	}

	@SuppressWarnings("rawtypes")
	public static class TestClient extends AbstractLoadBalancerAwareClient {

		private final RuntimeException exception;

		public TestClient(RuntimeException exception) {
			super(null);
			this.exception = exception;
		}

		@Override
		public IResponse executeWithLoadBalancer(final ClientRequest request,
				final IClientConfig requestConfig) throws ClientException {
			throw exception;
		}

		@Override
		public RequestSpecificRetryHandler getRequestSpecificRetryHandler(
				final ClientRequest clientRequest, final IClientConfig iClientConfig) {
			return null;
		}

		@Override
		public IResponse execute(final ClientRequest clientRequest,
				final IClientConfig iClientConfig) throws Exception {
			return null;
		}
	}

	public static class TestFallbackProvider implements FallbackProvider {

		private final ClientHttpResponse response;
		private Throwable cause;

		public TestFallbackProvider(final ClientHttpResponse response) {
			this.response = response;
		}

		@Override
		public ClientHttpResponse fallbackResponse(final Throwable cause) {
			this.cause = cause;
			return response;
		}

		@Override
		public String getRoute() {
			return "test-route";
		}

		@Override
		public ClientHttpResponse fallbackResponse() {
			throw new UnsupportedOperationException(
					"fallback without cause is not supported");
		}

		public Throwable getCause() {
			return cause;
		}

		public static TestFallbackProvider withResponse(final HttpStatus status) {
			return new TestFallbackProvider(getClientHttpResponse(status));
		}
	}

	public static class TestZuulFallbackProviderWithoutCause
			implements ZuulFallbackProvider {

		private final ClientHttpResponse response;

		public TestZuulFallbackProviderWithoutCause(final ClientHttpResponse response) {
			this.response = response;
		}

		public TestZuulFallbackProviderWithoutCause(final HttpStatus status) {
			this(getClientHttpResponse(status));
		}

		@Override
		public String getRoute() {
			return "test-route";
		}

		@Override
		public ClientHttpResponse fallbackResponse() {
			return response;
		}
	}

	private static ClientHttpResponse getClientHttpResponse(final HttpStatus status) {
		return new ClientHttpResponse() {
			@Override
			public HttpStatus getStatusCode() throws IOException {
				return status;
			}

			@Override
			public int getRawStatusCode() throws IOException {
				return getStatusCode().value();
			}

			@Override
			public String getStatusText() throws IOException {
				return getStatusCode().getReasonPhrase();
			}

			@Override
			public void close() {
			}

			@Override
			public InputStream getBody() throws IOException {
				return new ByteArrayInputStream("test".getBytes());
			}

			@Override
			public HttpHeaders getHeaders() {
				return new HttpHeaders();
			}
		};
	}
}
