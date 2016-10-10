package org.springframework.cloud.netflix.eureka.server;

import static org.junit.Assert.*;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;

import java.util.LinkedList;
import java.util.List;

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
import org.springframework.cloud.netflix.eureka.server.InstanceRegistryTest.Application;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceCanceledEvent;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRegisteredEvent;
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
@SpringBootTest(classes = Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        value = {"spring.application.name=eureka", "logging.level.org.springframework."
                + "cloud.netflix.eureka.server.InstanceRegistry=DEBUG"})
public class InstanceRegistryTest {

	private final List<ApplicationEvent> applicationEvents = new LinkedList<>();

	@SpyBean(PeerAwareInstanceRegistry.class)
	private InstanceRegistry instanceRegistry;

	@MockBean
	private ApplicationListener<EurekaInstanceRegisteredEvent>
			instanceRegisteredEventListenerMock;

	@MockBean
	private ApplicationListener<EurekaInstanceCanceledEvent>
			instanceCanceledEventListenerMock;

	@Before
	public void setup() {
		applicationEvents.clear();
		Answer applicationListenerAnswer = prepareListenerMockAnswer();
		doAnswer(applicationListenerAnswer).when(instanceRegisteredEventListenerMock)
				.onApplicationEvent(isA(EurekaInstanceRegisteredEvent.class));
		doAnswer(applicationListenerAnswer).when(instanceCanceledEventListenerMock)
				.onApplicationEvent(isA(EurekaInstanceCanceledEvent.class));
	}
		

	@Test
	public void testRegister() throws Exception {
		// creating instance info
		LeaseInfo leaseInfo = getLeaseInfo();
		InstanceInfo instanceInfo = getInstanceInfo(leaseInfo);
		// calling tested method
		instanceRegistry.register(instanceInfo, false);
		// event of proper type is registered
		assertEquals(1, applicationEvents.size());
		assertTrue(applicationEvents.get(0) instanceof EurekaInstanceRegisteredEvent);
		// event details are correct
		EurekaInstanceRegisteredEvent registeredEvent =
				(EurekaInstanceRegisteredEvent) (applicationEvents.get(0));
		assertEquals(instanceInfo, registeredEvent.getInstanceInfo());
		assertEquals(leaseInfo.getDurationInSecs(), registeredEvent.getLeaseDuration());
		assertEquals(instanceRegistry, registeredEvent.getSource());
		assertFalse(registeredEvent.isReplication());
	}

	@Test
	public void testDefaultLeaseDurationRegisterEvent() throws Exception {
		// creating instance info
		InstanceInfo instanceInfo = getInstanceInfo(null);
		// calling tested method
		instanceRegistry.register(instanceInfo, false);
		// instance info duration is set to default
		EurekaInstanceRegisteredEvent registeredEvent =
				(EurekaInstanceRegisteredEvent) (applicationEvents.get(0));
		assertEquals(LeaseInfo.DEFAULT_LEASE_DURATION,
				registeredEvent.getLeaseDuration());
	}

	@Test
	public void testInternalCancel() throws Exception {
		// calling tested method
		instanceRegistry.internalCancel("my-app", "appId", false);
		// event of proper type is registered
		assertEquals(1, applicationEvents.size());
		assertTrue(applicationEvents.get(0) instanceof EurekaInstanceCanceledEvent);
		// event details are correct
		EurekaInstanceCanceledEvent registeredEvent =
				(EurekaInstanceCanceledEvent) (applicationEvents.get(0));
		assertEquals("my-app", registeredEvent.getAppName());
		assertEquals("appId", registeredEvent.getServerId());
		assertEquals(instanceRegistry, registeredEvent.getSource());
		assertFalse(registeredEvent.isReplication());
	}

	@Configuration
	@EnableAutoConfiguration
	@EnableEurekaServer
	protected static class Application {
		public static void main(String[] args) {
			new SpringApplicationBuilder(Application.class).run(args);
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
		builder.setAppName("my-app-name");
		builder.setHostName("my-host-name");
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