/*
 * Copyright 2017-present the original author or authors.
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

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.netflix.discovery.shared.resolver.DefaultEndpoint;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

	@Test
	void testHostnameVerificationFailure() throws Exception {
		SelfSignedCertificate certificate = new SelfSignedCertificate("localhost");
		io.netty.handler.ssl.SslContext serverSslContext = SslContextBuilder
			.forServer(certificate.certificate(), certificate.privateKey())
			.build();

		AtomicBoolean hostnameVerified = new AtomicBoolean();

		DisposableServer server = HttpServer.create()
			.port(0)
			.secure(sslContextSpec -> sslContextSpec.sslContext(serverSslContext))
			.handle((request, response) -> response.send())
			.bindNow();

		try {
			SSLContext clientSslContext = SSLContext.getInstance("TLS");
			TrustManager[] trustManagers = { new X509TrustManager() {

				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return new X509Certificate[0];
				}

				@Override
				public void checkClientTrusted(X509Certificate[] chain, String authType) {
				}

				@Override
				public void checkServerTrusted(X509Certificate[] chain, String authType) {
				}

			} };
			clientSslContext.init(null, trustManagers, new SecureRandom());

			HostnameVerifier hostnameVerifier = (hostname, session) -> {
				hostnameVerified.set(true);
				return false;
			};

			WebClientTransportClientFactory factory = new WebClientTransportClientFactory(WebClient::builder,
					Optional.of(clientSslContext), Optional.of(hostnameVerifier));

			try {
				WebClientEurekaHttpClient client = (WebClientEurekaHttpClient) factory
					.newClient(new DefaultEndpoint("https://localhost:" + server.port()));

				assertThatThrownBy(() -> client.getWebClient()
					.get()
					.retrieve()
					.bodyToMono(Void.class)
					.block(Duration.ofSeconds(10))).hasRootCauseInstanceOf(SSLPeerUnverifiedException.class);

				assertThat(hostnameVerified).isTrue();
			}
			finally {
				factory.shutdown();
			}
		}
		finally {
			server.disposeNow();
			certificate.delete();
		}
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
