/*
 * Copyright 2016-2019 the original author or authors.
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.cloud.netflix.eureka.server.EurekaControllerTests.setInstance;

public class EurekaControllerReplicasTests {

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

	String empty = new String();

	private ApplicationInfoManager original;

	private InstanceInfo instanceInfo;

	@Before
	public void setup() throws Exception {
		this.original = ApplicationInfoManager.getInstance();
		setInstance(mock(ApplicationInfoManager.class));
		instanceInfo = mock(InstanceInfo.class);
	}

	@After
	public void teardown() throws Exception {
		setInstance(this.original);
		instanceInfo = null;
	}

	@Test
	public void testFilterReplicasNoAuth() throws Exception {
		Map<String, Object> model = new HashMap<>();
		StatusInfo statusInfo = StatusInfo.Builder.newBuilder()
				.add("registered-replicas", empty).add("available-replicas", noAuthList1)
				.add("unavailable-replicas", noAuthList2)
				.withInstanceInfo(this.instanceInfo).build();
		EurekaController controller = new EurekaController(null);

		controller.filterReplicas(model, statusInfo);

		@SuppressWarnings("unchecked")
		Map<String, String> results = (Map<String, String>) model.get("applicationStats");
		assertThat(results.get("registered-replicas")).isEqualTo(empty);
		assertThat(results.get("available-replicas")).isEqualTo(noAuthList1);
		assertThat(results.get("unavailable-replicas")).isEqualTo(noAuthList2);

	}

	@Test
	public void testFilterReplicasAuth() throws Exception {
		Map<String, Object> model = new HashMap<>();
		StatusInfo statusInfo = StatusInfo.Builder.newBuilder()
				.add("registered-replicas", authList2)
				.add("available-replicas", authList1).add("unavailable-replicas", empty)
				.withInstanceInfo(instanceInfo).build();
		EurekaController controller = new EurekaController(null);

		controller.filterReplicas(model, statusInfo);

		@SuppressWarnings("unchecked")
		Map<String, String> results = (Map<String, String>) model.get("applicationStats");
		assertThat(results.get("unavailable-replicas")).isEqualTo(empty);
		assertThat(results.get("available-replicas")).isEqualTo(noAuthList1);
		assertThat(results.get("registered-replicas")).isEqualTo(noAuthList2);

	}

	@Test
	public void testFilterReplicasAuthWithCombinationList() throws Exception {
		Map<String, Object> model = new HashMap<>();
		StatusInfo statusInfo = StatusInfo.Builder.newBuilder()
				.add("registered-replicas", totalAutoList)
				.add("available-replicas", combinationAuthList1)
				.add("unavailable-replicas", combinationAuthList2)
				.withInstanceInfo(instanceInfo).build();
		EurekaController controller = new EurekaController(null);

		controller.filterReplicas(model, statusInfo);

		@SuppressWarnings("unchecked")
		Map<String, String> results = (Map<String, String>) model.get("applicationStats");
		assertThat(results.get("registered-replicas")).isEqualTo(totalNoAutoList);
		assertThat(results.get("available-replicas")).isEqualTo(combinationNoAuthList1);
		assertThat(results.get("unavailable-replicas")).isEqualTo(combinationNoAuthList2);
	}

}
