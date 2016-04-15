package org.springframework.cloud.netflix.eureka.server;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;


import org.junit.Test;

import com.netflix.eureka.util.StatusInfo;


public class EurekaControllerReplicasTest {

	String noAuthList1 = "http://test1.com";
	String noAuthList2 = noAuthList1+",http://test2.com";

	String authList1 = "http://user:pwd@test1.com";
	String authList2 = authList1+",http://user2:pwd2@test2.com";
	
	String empty = new String();
	
	@Test
	public void testFilterReplicasNoAuth() throws Exception {
		Map<String, Object> model=new HashMap<String, Object>();
		StatusInfo statusInfo = StatusInfo.Builder.newBuilder().
									add("registered-replicas", empty).
									add("available-replicas",noAuthList1).
									add("unavailable-replicas",noAuthList2).
									build();
		EurekaController controller = new EurekaController(null);
		
		controller.filterReplicas(model,statusInfo);

		@SuppressWarnings("unchecked")
		Map<String,String> results = (Map<String, String>) model.get("applicationStats");
		assertEquals(empty,results.get("registered-replicas"));
		assertEquals(noAuthList1,results.get("available-replicas"));
		assertEquals(noAuthList2,results.get("unavailable-replicas"));
		
	}

	@Test
	public void testFilterReplicasAuth() throws Exception {
		Map<String, Object> model=new HashMap<String, Object>();
		StatusInfo statusInfo = StatusInfo.Builder.newBuilder().
									add("registered-replicas", authList2).
									add("available-replicas",authList1).
									add("unavailable-replicas",empty).
									build();
		EurekaController controller = new EurekaController(null);
		
		controller.filterReplicas(model,statusInfo);
		
		@SuppressWarnings("unchecked")
		Map<String,String> results = (Map<String, String>) model.get("applicationStats");
		assertEquals(empty,results.get("unavailable-replicas"));		
		assertEquals(noAuthList1,results.get("available-replicas"));
		assertEquals(noAuthList2,results.get("registered-replicas"));
		
	}
	
}
