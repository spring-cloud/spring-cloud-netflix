/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.netflix.turbine.stream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.contract.stubrunner.StubTrigger;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties.StubsMode;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.integration.support.management.MessageChannelMetrics;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author Spencer Gibb
 * @author Daniel Lavoie
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {
		// TODO: we don't need this if we harmonize the turbine and hystrix
		// destinations
		// https://github.com/spring-cloud/spring-cloud-netflix/issues/1948
		"spring.cloud.stream.bindings.turbineStreamInput.destination=hystrixStreamOutput",
		"spring.jmx.enabled=true", "stubrunner.workOffline=true",
		"stubrunner.ids=org.springframework.cloud:spring-cloud-netflix-hystrix-stream:${projectVersion:2.1.3.BUILD-SNAPSHOT}:stubs" })
@AutoConfigureStubRunner(stubsMode = StubsMode.LOCAL)
public class TurbineStreamTests {

	@Autowired
	StubTrigger stubTrigger;

	@Autowired
	ObjectMapper mapper;

	@Autowired
	@Qualifier(TurbineStreamClient.INPUT)
	SubscribableChannel input;

	RestTemplate rest = new RestTemplate();

	@Autowired
	TurbineStreamConfiguration turbine;

	@LocalServerPort
	int port;

	@Test
	public void contextLoads() throws Exception {
		rest.getInterceptors().add(new NonClosingInterceptor());
		int count = ((MessageChannelMetrics) input).getSendCount();
		ResponseEntity<String> response = rest.execute(
				new URI("http://localhost:" + port + "/"), HttpMethod.GET, null,
				this::extract);
		assertThat(response.getHeaders().getContentType()
				.isCompatibleWith(MediaType.TEXT_EVENT_STREAM)).isTrue();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		Map<String, Object> metrics = extractMetrics(response.getBody());
		assertThat(metrics).containsEntry("type", "HystrixCommand");
		assertThat(((MessageChannelMetrics) input).getSendCount()).isEqualTo(count + 1);
	}

	private boolean containsMetrics(String line) {
		return line.startsWith("data:") && !line.contains("ping");
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> extractMetrics(String body) throws Exception {
		for (String value : body.split("\n")) {
			if (containsMetrics(value)) {
				return mapper.readValue(value.split("data:")[1], Map.class);
			}
		}
		return null;
	}

	private ResponseEntity<String> extract(ClientHttpResponse response)
			throws IOException {
		// The message has to be sent after the endpoint is activated, so this is a
		// convenient place to put it
		stubTrigger.trigger("metrics");

		String responseBody = "";
		boolean metricFound = false;
		try (BufferedReader buffer = new BufferedReader(
				new InputStreamReader(response.getBody()))) {
			do {
				String line = buffer.readLine();
				responseBody += line + "\n";
				if (containsMetrics(line)) {
					metricFound = true;
				}
			}
			while (!metricFound);
		}

		return ResponseEntity.status(response.getStatusCode())
				.headers(response.getHeaders()).body(responseBody);
	}

	@EnableAutoConfiguration
	@EnableTurbineStream
	@Configuration
	public static class TestConfig {

	}

	/**
	 * Special interceptor that prevents the response from being closed and allows us to
	 * assert on the contents of an event stream.
	 */
	private class NonClosingInterceptor implements ClientHttpRequestInterceptor {

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body,
				ClientHttpRequestExecution execution) throws IOException {
			return new NonClosingResponse(execution.execute(request, body));
		}

		private class NonClosingResponse implements ClientHttpResponse {

			private ClientHttpResponse delegate;

			NonClosingResponse(ClientHttpResponse delegate) {
				this.delegate = delegate;
			}

			@Override
			public InputStream getBody() throws IOException {
				return delegate.getBody();
			}

			@Override
			public HttpHeaders getHeaders() {
				return delegate.getHeaders();
			}

			@Override
			public HttpStatus getStatusCode() throws IOException {
				return delegate.getStatusCode();
			}

			@Override
			public int getRawStatusCode() throws IOException {
				return delegate.getRawStatusCode();
			}

			@Override
			public String getStatusText() throws IOException {
				return delegate.getStatusText();
			}

			@Override
			public void close() {
			}

		}

	}

}
