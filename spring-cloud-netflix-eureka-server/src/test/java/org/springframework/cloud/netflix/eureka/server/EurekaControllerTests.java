/*
 * Copyright 2013-2016 the original author or authors.
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

package org.springframework.cloud.netflix.eureka.server;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.ReflectionUtils;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.MyDataCenterInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.eureka.EurekaServerContext;
import com.netflix.eureka.EurekaServerContextHolder;
import com.netflix.eureka.cluster.PeerEurekaNode;
import com.netflix.eureka.cluster.PeerEurekaNodes;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EurekaControllerTests {

	private ApplicationInfoManager infoManager;
	private ApplicationInfoManager original;

	@Before
	public void setup() throws Exception {
		PeerEurekaNodes peerEurekaNodes = mock(PeerEurekaNodes.class);
		when(peerEurekaNodes.getPeerNodesView()).thenReturn(Collections.<PeerEurekaNode>emptyList());

		InstanceInfo instanceInfo = InstanceInfo.Builder.newBuilder()
				.setAppName("test")
				.setDataCenterInfo(new MyDataCenterInfo(DataCenterInfo.Name.MyOwn))
				.build();

		this.infoManager = mock(ApplicationInfoManager.class);
		this.original = ApplicationInfoManager.getInstance();
		setInstance(this.infoManager);
		when(this.infoManager.getInfo()).thenReturn(instanceInfo);

		Application myapp = new Application("myapp");
		myapp.addInstance(InstanceInfo.Builder.newBuilder()
				.setAppName("myapp")
				.setDataCenterInfo(new MyDataCenterInfo(DataCenterInfo.Name.MyOwn))
				.setInstanceId("myapp:1")
				.build());

		ArrayList<Application> applications = new ArrayList<>();
		applications.add(myapp);

		PeerAwareInstanceRegistry registry = mock(PeerAwareInstanceRegistry.class);
		when(registry.getSortedApplications()).thenReturn(applications);

		EurekaServerContext serverContext = mock(EurekaServerContext.class);
		EurekaServerContextHolder.initialize(serverContext);
		when(serverContext.getRegistry()).thenReturn(registry);
		when(serverContext.getPeerEurekaNodes()).thenReturn(peerEurekaNodes);
		when(serverContext.getApplicationInfoManager()).thenReturn(this.infoManager);

	}

	@After
	public void teardown() throws Exception {
		setInstance(this.original);
	}

	static void setInstance(ApplicationInfoManager infoManager) throws IllegalAccessException {
		Field instance = ReflectionUtils.findField(ApplicationInfoManager.class, "instance");
		ReflectionUtils.makeAccessible(instance);
		instance.set(null, infoManager);
	}

	@Test
	public void testStatus() throws Exception {
		Map<String, Object> model = new HashMap<>();

		EurekaController controller = new EurekaController(infoManager);

		controller.status(new MockHttpServletRequest("GET", "/"), model);

		Map<String, Object> app = getFirst(model, "apps");
		Map<String, Object> instanceInfo = getFirst(app, "instanceInfos");
		Map<String, Object> instance = getFirst(instanceInfo, "instances");

		assertThat("id was wrong", (String)instance.get("id"), is(equalTo("myapp:1")));
		assertThat("url was not null", instance.get("url"), is(nullValue()));
		assertThat("isHref was wrong", (Boolean)instance.get("isHref"), is(false));
	}

	@SuppressWarnings("unchecked")
	Map<String, Object> getFirst(Map<String, Object> model, String key) {
		List<Map<String, Object>> apps = (List<Map<String, Object>>) model.get(key);
		assertThat(key +" was wrong size", apps, is(hasSize(1)));
		return apps.get(0);
	}

}
