package org.springframework.cloud.netflix.eureka.server;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.springframework.cloud.netflix.eureka.server.EurekaControllerTests.setInstance;

import java.util.HashMap;
import java.util.Map;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.netflix.eureka.util.StatusInfo;

public class EurekaControllerReplicasTests {

	String noAuthList1 = "http://test1.com";
	String noAuthList2 = noAuthList1 + ",http://test2.com";

	String authList1 = "http://user:pwd@test1.com";
	String authList2 = authList1 + ",http://user2:pwd2@test2.com";

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
				.add("registered-replicas", empty)
				.add("available-replicas", noAuthList1)
				.add("unavailable-replicas", noAuthList2)
				.withInstanceInfo(this.instanceInfo).build();
		EurekaController controller = new EurekaController(null);

		controller.filterReplicas(model, statusInfo);

		@SuppressWarnings("unchecked")
		Map<String, String> results = (Map<String, String>) model.get("applicationStats");
		assertEquals(empty, results.get("registered-replicas"));
		assertEquals(noAuthList1, results.get("available-replicas"));
		assertEquals(noAuthList2, results.get("unavailable-replicas"));

	}

	@Test
	public void testFilterReplicasAuth() throws Exception {
		Map<String, Object> model = new HashMap<>();
		StatusInfo statusInfo = StatusInfo.Builder.newBuilder()
				.add("registered-replicas", authList2)
				.add("available-replicas", authList1)
				.add("unavailable-replicas", empty)
				.withInstanceInfo(instanceInfo).build();
		EurekaController controller = new EurekaController(null);

		controller.filterReplicas(model, statusInfo);

		@SuppressWarnings("unchecked")
		Map<String, String> results = (Map<String, String>) model.get("applicationStats");
		assertEquals(empty, results.get("unavailable-replicas"));
		assertEquals(noAuthList1, results.get("available-replicas"));
		assertEquals(noAuthList2, results.get("registered-replicas"));

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
		assertEquals(totalNoAutoList, results.get("registered-replicas"));
		assertEquals(combinationNoAuthList1, results.get("available-replicas"));
		assertEquals(combinationNoAuthList2, results.get("unavailable-replicas"));

	}

}
