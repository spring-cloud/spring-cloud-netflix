/*
 * Copyright 2012-2024 the original author or authors.
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

import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.discovery.shared.Applications;
import com.netflix.discovery.shared.transport.EurekaHttpClient;
import com.netflix.discovery.shared.transport.EurekaHttpResponse;

import io.micrometer.observation.Observation;
import io.micrometer.observation.tck.ObservationContextAssert;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.observation.ClientHttpObservationDocumentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.assertj.core.api.InstanceOfAssertFactories;

/**
 * @author Haytham Mohamed
 * @author Václav Plic
 **/
abstract class AbstractEurekaHttpClientTests {

	protected EurekaHttpClient eurekaHttpClient;

	protected InstanceInfo info;

	protected final TestObservationRegistry observationRegistry = TestObservationRegistry.create();

	abstract void setup();

	@Test
	void testRegister() {
		assertThat(eurekaHttpClient.register(info).getStatusCode()).isEqualTo(HttpStatus.OK.value());

		assertThatClientHttpObservationUriKeyValueDoesNotContainVariableStrings("test");
	}

	@Test
	void testCancel() {
		assertThat(eurekaHttpClient.cancel("test", "test").getStatusCode()).isEqualTo(HttpStatus.OK.value());

		assertThatClientHttpObservationUriKeyValueDoesNotContainVariableStrings("test");
	}

	@ParameterizedTest
	@ValueSource(strings = { "test", "test#1.[3.?]!" })
	void testSendHeartBeat(String instanceId) {
		EurekaHttpResponse<InstanceInfo> response = eurekaHttpClient.sendHeartBeat("test", instanceId, info, null);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());
		assertThat(response.getEntity()).isNotNull();

		assertThatClientHttpObservationUriKeyValueDoesNotContainVariableStrings(instanceId);
	}

	@Test
	void testSendHeartBeatFourOFour() {
		assertThat(eurekaHttpClient.sendHeartBeat("fourOFour", "test", info, null).getStatusCode())
			.isEqualTo(HttpStatus.NOT_FOUND.value());

		assertThatClientHttpObservationUriKeyValueDoesNotContainVariableStrings("fourOFour", "test");
	}

	@Test
	void testSendHeartBeatFourOFourWithBody() {
		assertThat(eurekaHttpClient.sendHeartBeat("fourOFourWithBody", "test", info, null).getStatusCode())
			.isEqualTo(HttpStatus.NOT_FOUND.value());

		assertThatClientHttpObservationUriKeyValueDoesNotContainVariableStrings("fourOFourWithBody", "test");
	}

	@ParameterizedTest
	@ValueSource(strings = { "test", "test#1.[3.?]!" })
	void testStatusUpdate(String instanceId) {
		assertThat(
				eurekaHttpClient.statusUpdate("test", instanceId, InstanceInfo.InstanceStatus.UP, info).getStatusCode())
			.isEqualTo(HttpStatus.OK.value());

		assertThatClientHttpObservationUriKeyValueDoesNotContainVariableStrings("test", instanceId);
	}

	@Test
	void testDeleteStatusOverride() {
		assertThat(eurekaHttpClient.deleteStatusOverride("test", "test", info).getStatusCode())
			.isEqualTo(HttpStatus.OK.value());

		assertThatClientHttpObservationUriKeyValueDoesNotContainVariableStrings("test");
	}

	@Test
	void testGetApplications() {
		Applications entity = eurekaHttpClient.getApplications().getEntity();
		assertThat(entity).isNotNull();
		assertThat(eurekaHttpClient.getApplications("us", "eu").getEntity()).isNotNull();

		assertThatClientHttpObservationUriKeyValueDoesNotContainVariableStrings("us,eu");
	}

	@Test
	void testGetDelta() {
		eurekaHttpClient.getDelta().getEntity();
		eurekaHttpClient.getDelta("us", "eu").getEntity();

		assertThatClientHttpObservationUriKeyValueDoesNotContainVariableStrings("us,eu");
	}

	@Test
	void testGetVips() {
		eurekaHttpClient.getVip("test");
		eurekaHttpClient.getVip("test", "us", "eu");

		assertThatClientHttpObservationUriKeyValueDoesNotContainVariableStrings("test", "us,eu");
	}

	@Test
	void testGetSecureVip() {
		eurekaHttpClient.getSecureVip("test");
		eurekaHttpClient.getSecureVip("test", "us,eu");

		assertThatClientHttpObservationUriKeyValueDoesNotContainVariableStrings("test", "us,eu");
	}

	@Test
	void testGetApplication() {
		eurekaHttpClient.getApplication("test");

		assertThatClientHttpObservationUriKeyValueDoesNotContainVariableStrings("test");
	}

	@ParameterizedTest
	@ValueSource(strings = { "test", "test#1.[3.?]!" })
	void testGetInstance(String instanceId) {
		eurekaHttpClient.getInstance(instanceId);
		eurekaHttpClient.getInstance("test", instanceId);

		assertThatClientHttpObservationUriKeyValueDoesNotContainVariableStrings(instanceId);
	}

	/**
	 * Verifies that the client request observations that are created by the eureka client
	 * implementation do not have the URI key-value containing anything that could be
	 * considered high-cardinality like the lastDirtyTimestamp or an instance id.
	 * @param values Strings that should not appear in the URI key-value in addition to
	 * the common ones. Must not be empty.
	 */
	private void assertThatClientHttpObservationUriKeyValueDoesNotContainVariableStrings(CharSequence... values) {
		TestObservationRegistryAssert.assertThat(this.observationRegistry)
			.forAllObservationsWithNameEqualTo("http.client.requests", c -> ((ObservationContextAssert<?>) c)
				.asInstanceOf(InstanceOfAssertFactories.type(Observation.Context.class))
				.extracting(context -> context
					.getLowCardinalityKeyValue(ClientHttpObservationDocumentation.LowCardinalityKeyNames.URI.asString())
					.getValue())
				.asString()
				.doesNotContain(Set.of(info.getAppName(), info.getLastDirtyTimestamp().toString()))
				.doesNotContain(ArrayUtils.toStringArray(InstanceStatus.values()))
				.doesNotContain(values));
	}

}
