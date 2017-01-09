package org.springframework.cloud.netflix.eureka.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.cloud.netflix.eureka.server.InstanceRegistryTest.TestApplication;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceCanceledEvent;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRegisteredEvent;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRenewedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.LeaseInfo;
import com.netflix.discovery.shared.Application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author Bartlomiej Slota
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(TestApplication.class)
@WebIntegrationTest(randomPort = true,
		value = {"spring.application.name=eureka", "logging.level.org.springframework."
		+ "cloud.netflix.eureka.server.InstanceRegistry=DEBUG"})
public class InstanceRegistryTest {

	private final List<ApplicationEvent> applicationEvents = new LinkedList<>();
	private static final String APP_NAME = "MY-APP-NAME";
	private static final String HOST_NAME = "my-host-name";
	private static final String INSTANCE_ID = "my-host-name:8008";
	private static final int PORT = 8008;

	@Autowired
	private InstanceRegistry originalInstanceRegistry;

	private InstanceRegistry instanceRegistry;

	@Autowired
	private ApplicationListener<EurekaInstanceRegisteredEvent>
			instanceRegisteredEventListenerMock;

	@Autowired
	private ApplicationListener<EurekaInstanceCanceledEvent>
			instanceCanceledEventListenerMock;

	@Autowired
	private ApplicationListener<EurekaInstanceRenewedEvent> instanceRenewedEventListener;

	@Before
	public void setup() {
		applicationEvents.clear();

		this.instanceRegistry = Mockito.spy(originalInstanceRegistry);

		Answer applicationListenerAnswer = prepareListenerMockAnswer();
		doAnswer(applicationListenerAnswer).when(instanceRegisteredEventListenerMock)
				.onApplicationEvent(isA(EurekaInstanceRegisteredEvent.class));
		doAnswer(applicationListenerAnswer).when(instanceCanceledEventListenerMock)
				.onApplicationEvent(isA(EurekaInstanceCanceledEvent.class));
		doAnswer(applicationListenerAnswer).when(instanceRenewedEventListener)
				.onApplicationEvent(isA(EurekaInstanceRenewedEvent.class));
	}
		

	@Test
	public void testRegister() throws Exception {
		// creating instance info
		final LeaseInfo leaseInfo = getLeaseInfo();
		final InstanceInfo instanceInfo = getInstanceInfo(APP_NAME, HOST_NAME, INSTANCE_ID, PORT, leaseInfo);
		// calling tested method
		instanceRegistry.register(instanceInfo, false);
		// event of proper type is registered
		assertEquals(1, applicationEvents.size());
		assertTrue(applicationEvents.get(0) instanceof EurekaInstanceRegisteredEvent);
		// event details are correct
		final EurekaInstanceRegisteredEvent registeredEvent =
				(EurekaInstanceRegisteredEvent) (applicationEvents.get(0));
		assertEquals(instanceInfo, registeredEvent.getInstanceInfo());
		assertEquals(leaseInfo.getDurationInSecs(), registeredEvent.getLeaseDuration());
		assertEquals(instanceRegistry, registeredEvent.getSource());
		assertFalse(registeredEvent.isReplication());
	}

	@Test
	public void testDefaultLeaseDurationRegisterEvent() throws Exception {
		// creating instance info
		final InstanceInfo instanceInfo = getInstanceInfo(APP_NAME, HOST_NAME, INSTANCE_ID, PORT, null);
		// calling tested method
		instanceRegistry.register(instanceInfo, false);
		// instance info duration is set to default
		final EurekaInstanceRegisteredEvent registeredEvent =
				(EurekaInstanceRegisteredEvent) (applicationEvents.get(0));
		assertEquals(LeaseInfo.DEFAULT_LEASE_DURATION,
				registeredEvent.getLeaseDuration());
	}

	@Test
	public void testInternalCancel() throws Exception {
		// calling tested method
		instanceRegistry.internalCancel(APP_NAME, HOST_NAME, false);
		// event of proper type is registered
		assertEquals(1, applicationEvents.size());
		assertTrue(applicationEvents.get(0) instanceof EurekaInstanceCanceledEvent);
		// event details are correct
		final EurekaInstanceCanceledEvent registeredEvent =
				(EurekaInstanceCanceledEvent) (applicationEvents.get(0));
		assertEquals(APP_NAME, registeredEvent.getAppName());
		assertEquals(HOST_NAME, registeredEvent.getServerId());
		assertEquals(instanceRegistry, registeredEvent.getSource());
		assertFalse(registeredEvent.isReplication());
	}

	@Test
	public void testRenew() throws Exception {
		//Creating two instances of the app
		final InstanceInfo instanceInfo1 = getInstanceInfo(APP_NAME, HOST_NAME, INSTANCE_ID, PORT, null);
		final InstanceInfo instanceInfo2 = getInstanceInfo(APP_NAME, HOST_NAME, "my-host-name:8009", 8009, null);
		// creating application list with an app having two instances
		final Application application = new Application(APP_NAME, Arrays.asList(instanceInfo1, instanceInfo2));
		final List<Application> applications = new ArrayList<>();
		applications.add(application);
		// stubbing applications list
		doReturn(applications).when(instanceRegistry).getSortedApplications();
		// calling tested method
		instanceRegistry.renew(APP_NAME, INSTANCE_ID, false);
		instanceRegistry.renew(APP_NAME, "my-host-name:8009", false);
		// event of proper type is registered
		assertEquals(2, applicationEvents.size());
		assertTrue(applicationEvents.get(0) instanceof EurekaInstanceRenewedEvent);
		assertTrue(applicationEvents.get(1) instanceof EurekaInstanceRenewedEvent);
		// event details are correct
		final EurekaInstanceRenewedEvent event1 = (EurekaInstanceRenewedEvent)
				(applicationEvents.get(0));
		assertEquals(APP_NAME, event1.getAppName());
		assertEquals(INSTANCE_ID, event1.getServerId());
		assertEquals(instanceRegistry, event1.getSource());
		assertEquals(instanceInfo1, event1.getInstanceInfo());
		assertFalse(event1.isReplication());

		final EurekaInstanceRenewedEvent event2 = (EurekaInstanceRenewedEvent)
				(applicationEvents.get(1));
		assertEquals(instanceInfo2, event2.getInstanceInfo());
	}

	@Configuration
	@EnableAutoConfiguration
	@EnableEurekaServer
	@SuppressWarnings("unchecked")
	protected static class TestApplication {
		@Bean
		public ApplicationListener<EurekaInstanceRegisteredEvent> instanceRegisteredEventListenerMock() {
			return mock(ApplicationListener.class);
		}

		@Bean
		public ApplicationListener<EurekaInstanceCanceledEvent> instanceCanceledEventListenerMock() {
			return mock(ApplicationListener.class);
		}

		@Bean
		public ApplicationListener<EurekaInstanceRenewedEvent> instanceRenewedEventListener() {
			return mock(ApplicationListener.class);
		}

		public static void main(String[] args) {
			new SpringApplicationBuilder(TestApplication.class).run(args);
		}
	}

	private LeaseInfo getLeaseInfo() {
		LeaseInfo.Builder leaseBuilder = LeaseInfo.Builder.newBuilder();
		leaseBuilder.setRenewalIntervalInSecs(10);
		leaseBuilder.setDurationInSecs(15);
		return leaseBuilder.build();
	}

	private InstanceInfo getInstanceInfo(String appName, String hostName,
			String instanceId, int port, LeaseInfo leaseInfo) {
		InstanceInfo.Builder builder = InstanceInfo.Builder.newBuilder();
		builder.setAppName(appName);
		builder.setHostName(hostName);
		builder.setInstanceId(instanceId);
		builder.setPort(port);
		builder.setLeaseInfo(leaseInfo);
		return builder.build();
	}

	private Answer prepareListenerMockAnswer() {
		return new Answer() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return applicationEvents
						.add((ApplicationEvent) invocation.getArguments()[0]);
			}
		};
	}
}
