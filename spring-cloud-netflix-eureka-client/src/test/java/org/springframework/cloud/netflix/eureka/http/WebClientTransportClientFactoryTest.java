/*
 * Copyright 2017-2024 the original author or authors.
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

package org.springframework.cloud.netflix.eureka.http;

import java.time.Duration;

import com.netflix.discovery.shared.resolver.DefaultEndpoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Daniel Lavoie
 * @author Armin Krezovic
 */
@MockitoSettings(strictness = Strictness.LENIENT)
class WebClientTransportClientFactoryTest {

	@Mock
	private ExchangeFunction exchangeFunction;

	@Captor
	private ArgumentCaptor<ClientRequest> captor;

	private WebClientTransportClientFactory transportClientFatory;

	@BeforeEach
	void setup() {
		ClientResponse mockResponse = mock();
		when(mockResponse.statusCode()).thenReturn(HttpStatus.OK);
		when(mockResponse.bodyToMono(Void.class)).thenReturn(Mono.empty());
		given(exchangeFunction.exchange(captor.capture())).willReturn(Mono.just(mockResponse));

		transportClientFatory = new WebClientTransportClientFactory(
				() -> WebClient.builder().exchangeFunction(exchangeFunction));
	}

	@Test
	void testWithoutUserInfo() {
		transportClientFatory.newClient(new DefaultEndpoint("http://localhost:8761"));
	}

	@Test
	void testInvalidUserInfo() {
		transportClientFatory.newClient(new DefaultEndpoint("http://test@localhost:8761"));
	}

	@Test
	void testUserInfoWithEncodedCharacters() {
		String encodedBasicAuth = HttpHeaders.encodeBasicAuth("test", "MyPassword@", null);
		String expectedAuthHeader = "Basic " + encodedBasicAuth;
		String expectedUrl = "http://localhost:8761";

		WebClientEurekaHttpClient client = (WebClientEurekaHttpClient) transportClientFatory
			.newClient(new DefaultEndpoint("http://test:MyPassword%40@localhost:8761"));

		client.getWebClient().get().retrieve().bodyToMono(Void.class).block(Duration.ofSeconds(10));

		ClientRequest request = verifyAndGetRequest();

		assertThat(request.headers().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo(expectedAuthHeader);
		assertThat(request.url().toString()).isEqualTo(expectedUrl);
	}

	@Test
	void testUserInfo() {
		transportClientFatory.newClient(new DefaultEndpoint("http://test:test@localhost:8761"));
	}

	@AfterEach
	void shutdown() {
		transportClientFatory.shutdown();
	}

	private ClientRequest verifyAndGetRequest() {
		ClientRequest request = captor.getValue();
		verify(exchangeFunction).exchange(request);
		verifyNoMoreInteractions(exchangeFunction);
		return request;
	}

}
