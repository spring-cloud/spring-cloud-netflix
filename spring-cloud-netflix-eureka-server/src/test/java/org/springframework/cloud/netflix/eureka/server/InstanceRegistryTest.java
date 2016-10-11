package org.springframework.cloud.netflix.eureka.server;

import static org.junit.Assert.*;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.netflix.discovery.shared.Application;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.netflix.eureka.server.InstanceRegistryTest.TestApplication;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceCanceledEvent;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRegisteredEvent;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRenewedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.LeaseInfo;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;

/**
 * @author Bartlomiej Slota
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        value = {"spring.application.name=eureka", "logging.level.org.springframework."
                + "cloud.netflix.eureka.server.InstanceRegistry=DEBUG"})
public class InstanceRegistryTest {

	private final List<ApplicationEvent> applicationEvents = new LinkedList<>();
	private static final String APP_NAME = "MY-APP-NAME";
	private static final String HOST_NAME = "my-host-name";

	@SpyBean(PeerAwareInstanceRegistry.class)
	private InstanceRegistry instanceRegistry;

	@MockBean
	private ApplicationListener<EurekaInstanceRegisteredEvent>
			instanceRegisteredEventListenerMock;

	@MockBean
	private ApplicationListener<EurekaInstanceCanceledEvent>
			instanceCanceledEventListenerMock;

	@MockBean
	private ApplicationListener<EurekaInstanceRenewedEvent> instanceRenewedEventListener;

	@Before
	public void setup() {
		applicationEvents.clear();
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
		final InstanceInfo instanceInfo = getInstanceInfo(leaseInfo);
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
		final InstanceInfo instanceInfo = getInstanceInfo(null);
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
		// creating application list
		final LeaseInfo leaseInfo = getLeaseInfo();
		final InstanceInfo instanceInfo = getInstanceInfo(leaseInfo);
		final List<InstanceInfo> instances = new ArrayList<>();
		instances.add(instanceInfo);
		final Application application = new Application(APP_NAME, instances);
		final List<Application> applications = new ArrayList<>();
		applications.add(application);
		// stubbing applications list
		doReturn(applications).when(instanceRegistry).getSortedApplications();
		// calling tested method
		instanceRegistry.renew(APP_NAME, HOST_NAME, false);
		// event of proper type is registered
		assertEquals(1, applicationEvents.size());
		assertTrue(applicationEvents.get(0) instanceof EurekaInstanceRenewedEvent);
		// event details are correct
		final EurekaInstanceRenewedEvent registeredEvent = (EurekaInstanceRenewedEvent)
				(applicationEvents.get(0));
		assertEquals(APP_NAME, registeredEvent.getAppName());
		assertEquals(HOST_NAME, registeredEvent.getServerId());
		assertEquals(instanceRegistry, registeredEvent.getSource());
		assertEquals(instanceInfo, registeredEvent.getInstanceInfo());
		assertFalse(registeredEvent.isReplication());
	}

	@Configuration
	@EnableAutoConfiguration
	@EnableEurekaServer
	protected static class TestApplication {
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

	private InstanceInfo getInstanceInfo(LeaseInfo leaseInfo) {
		InstanceInfo.Builder builder = InstanceInfo.Builder.newBuilder();
		builder.setAppName(APP_NAME);
		builder.setHostName(HOST_NAME);
		builder.setPort(8008);
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