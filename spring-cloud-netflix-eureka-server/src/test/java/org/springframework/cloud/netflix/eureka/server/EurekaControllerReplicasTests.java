/*
 * Copyright 2016-present the original author or authors.
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

package org.springframework.cloud.netflix.eureka.server;

import java.util.HashMap;
import java.util.Map;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.eureka.util.StatusInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.cloud.netflix.eureka.server.EurekaControllerTests.setInstance;

class EurekaControllerReplicasTests {

	String noAuthList1 = "https://test1.com";

	String noAuthList2 = noAuthList1 + ",https://test2.com";

	String authList1 = "https://user:pwd@test1.com";

	String authList2 = authList1 + ",https://user2:pwd2@test2.com";

	String combinationAuthList1 = "http://test1.com,http://user2:pwd2@test2.com";

	String combinationAuthList2 = "http://test3.com,http://user4:pwd4@test4.com";

	String combinationNoAuthList1 = "http://test1.com,http://test2.com";

	String combinationNoAuthList2 = "http://test3.com,http://test4.com";

	String totalAutoList = combinationAuthList1 + "," + combinationAuthList2;

	String totalNoAutoList = combinationNoAuthList1 + "," + combinationNoAuthList2;

	String empty = "";

	private ApplicationInfoManager original;

	private InstanceInfo instanceInfo;

	@BeforeEach
	void setup() throws Exception {
		this.original = ApplicationInfoManager.getInstance();
		setInstance(mock(ApplicationInfoManager.class));
		instanceInfo = mock(InstanceInfo.class);
	}

	@AfterEach
	void teardown() throws Exception {
		setInstance(this.original);
		instanceInfo = null;
	}

	@Test
	void testFilterReplicasNoAuth() {
		Map<String, Object> model = new HashMap<>();
		StatusInfo statusInfo = StatusInfo.Builder.newBuilder()
			.add("registered-replicas", empty)
			.add("available-replicas", noAuthList1)
			.add("unavailable-replicas", noAuthList2)
			.withInstanceInfo(this.instanceInfo)
			.build();
		EurekaController controller = new EurekaController(null, new EurekaProperties());

		controller.filterReplicas(model, statusInfo);

		@SuppressWarnings("unchecked")
		Map<String, String> results = (Map<String, String>) model.get("applicationStats");
		assertThat(results.get("registered-replicas")).isEqualTo(empty);
		assertThat(results.get("available-replicas")).isEqualTo(noAuthList1);
		assertThat(results.get("unavailable-replicas")).isEqualTo(noAuthList2);

	}

	@Test
	void testFilterReplicasAuth() {
		Map<String, Object> model = new HashMap<>();
		StatusInfo statusInfo = StatusInfo.Builder.newBuilder()
			.add("registered-replicas", authList2)
			.add("available-replicas", authList1)
			.add("unavailable-replicas", empty)
			.withInstanceInfo(instanceInfo)
			.build();
		EurekaController controller = new EurekaController(null, new EurekaProperties());

		controller.filterReplicas(model, statusInfo);

		@SuppressWarnings("unchecked")
		Map<String, String> results = (Map<String, String>) model.get("applicationStats");
		assertThat(results.get("unavailable-replicas")).isEqualTo(empty);
		assertThat(results.get("available-replicas")).isEqualTo(noAuthList1);
		assertThat(results.get("registered-replicas")).isEqualTo(noAuthList2);

	}

	@Test
	void testFilterReplicasAuthWithCombinationList() {
		Map<String, Object> model = new HashMap<>();
		StatusInfo statusInfo = StatusInfo.Builder.newBuilder()
			.add("registered-replicas", totalAutoList)
			.add("available-replicas", combinationAuthList1)
			.add("unavailable-replicas", combinationAuthList2)
			.withInstanceInfo(instanceInfo)
			.build();
		EurekaController controller = new EurekaController(null, new EurekaProperties());

		controller.filterReplicas(model, statusInfo);

		@SuppressWarnings("unchecked")
		Map<String, String> results = (Map<String, String>) model.get("applicationStats");
		assertThat(results.get("registered-replicas")).isEqualTo(totalNoAutoList);
		assertThat(results.get("available-replicas")).isEqualTo(combinationNoAuthList1);
		assertThat(results.get("unavailable-replicas")).isEqualTo(combinationNoAuthList2);
	}

	@ParameterizedTest(name = "[{index}] {0} -> {1}")
	@CsvSource(delimiter = '|', textBlock = """
			# credentials are removed, the rest of the URL is left untouched
			https://user:pwd@test1.com                          | https://test1.com
			https://user:pwd@test1.com:8761/eureka/             | https://test1.com:8761/eureka/
			https://user@test1.com:8761/eureka                  | https://test1.com:8761/eureka
			http://user:pwd@localhost:8761/eureka/#frag         | http://localhost:8761/eureka/#frag
			https://user:p%40ssw0rd@test1.com/eureka/           | https://test1.com/eureka/
			https://user:pwd@[::1]:8761/eureka                  | https://[::1]:8761/eureka
			# a peer URL has no legitimate use for a query string, and it may itself carry
			# credentials, so it is dropped rather than guessing which parameters are sensitive
			http://host:8761/path?user=admin&password=secret    | http://host:8761/path
			http://host:8761/path?a=b                           | http://host:8761/path
			# an unencoded '@' in the password must not leak the remainder of the password
			https://user:p@ss@test1.com/eureka                  | https://test1.com/eureka
			# an '@' outside of the user info must not truncate the URL
			https://test1.com/path@weird                        | https://test1.com/path@weird
			https://test1.com/eureka                            | https://test1.com/eureka
			# without a host the authority cannot be told apart from the rest of the URL,
			# so the URL is dropped rather than risk exposing credentials
			user:pass@host:8761/eureka/                         | ''
			not a url @ all                                     | ''
			# a URL that cannot be parsed is dropped for the same reason
			https://user:pwd@te st1.com                         | ''
			""")
	void scrubsUserInfoFromSingleUrl(String url, String expected) {
		assertThat(filterAvailableReplicas(url)).isEqualTo(expected);
	}

	@Test
	void testFilterReplicasScrubsEachUrlInList() {
		String urls = "https://user:p@ss@test1.com,https://test2.com/path@weird,https://user3@test3.com:8761";
		assertThat(filterAvailableReplicas(urls))
			.isEqualTo("https://test1.com,https://test2.com/path@weird,https://test3.com:8761");
	}

	private String filterAvailableReplicas(String availableReplicas) {
		Map<String, Object> model = new HashMap<>();
		StatusInfo statusInfo = StatusInfo.Builder.newBuilder()
			.add("registered-replicas", empty)
			.add("available-replicas", availableReplicas)
			.add("unavailable-replicas", empty)
			.withInstanceInfo(instanceInfo)
			.build();
		new EurekaController(null, new EurekaProperties()).filterReplicas(model, statusInfo);

		@SuppressWarnings("unchecked")
		Map<String, String> results = (Map<String, String>) model.get("applicationStats");
		return results.get("available-replicas");
	}

}
