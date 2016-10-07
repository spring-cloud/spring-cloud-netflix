package org.springframework.cloud.netflix.eureka.server;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.netflix.eureka.server.InstanceRegistryTest.Application;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceCanceledEvent;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRegisteredEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
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

    @SpyBean(PeerAwareInstanceRegistry.class)
    private InstanceRegistry instanceRegistry;

    @Autowired
    private Listener eurekaEventListener;

    @Before
    public void setup() {
        eurekaEventListener.getApplicationEvents().clear();
    }

    @Test
    public void testRegister() throws Exception {
        // stubbing superclass method invocation
        doNothing().when(instanceRegistry)
                .superRegister(any(InstanceInfo.class), anyBoolean());

        // creating instance info
        LeaseInfo leaseInfo = getLeaseInfo();
        InstanceInfo instanceInfo = getInstanceInfo(leaseInfo);

        // calling tested method
        instanceRegistry.register(instanceInfo, false);

        // event of proper type is registered
        assertEquals(1, eurekaEventListener.getApplicationEvents().size());
        assertTrue(eurekaEventListener.getApplicationEvents().get(0)
                instanceof EurekaInstanceRegisteredEvent);

        // event details are correct
        EurekaInstanceRegisteredEvent registeredEvent = (EurekaInstanceRegisteredEvent)
                (eurekaEventListener.getApplicationEvents().get(0));
        assertEquals(instanceInfo, registeredEvent.getInstanceInfo());
        assertEquals(leaseInfo.getDurationInSecs(), registeredEvent.getLeaseDuration());
        assertEquals(instanceRegistry, registeredEvent.getSource());
        assertFalse(registeredEvent.isReplication());

        // superclass method wrapper was successfully invoked
        verify(instanceRegistry).superRegister(instanceInfo, false);
    }

    @Test
    public void testDefaultLeaseDurationRegisterEvent() throws Exception {
        // stubbing superclass method invocation
        doNothing().when(instanceRegistry)
                .superRegister(any(InstanceInfo.class), anyBoolean());

        // creating instance info
        InstanceInfo instanceInfo = getInstanceInfo(null);

        // calling tested method
        instanceRegistry.register(instanceInfo, false);

        // instance info duration is set to default
        EurekaInstanceRegisteredEvent registeredEvent = (EurekaInstanceRegisteredEvent)
                (eurekaEventListener.getApplicationEvents().get(0));
        assertEquals(LeaseInfo.DEFAULT_LEASE_DURATION, registeredEvent.getLeaseDuration());
    }

    @Test
    public void testInternalCancel() throws Exception {
        // stubbing superclass method invocation
        doReturn(Boolean.TRUE).when(instanceRegistry)
                .superInternalCancel(anyString(), anyString(), anyBoolean());

        // calling tested method
        boolean cancellationResult = instanceRegistry
                .internalCancel("my-app", "appId", false);

        // event of proper type is registered
        assertEquals(1, eurekaEventListener.getApplicationEvents().size());
        assertTrue(eurekaEventListener.getApplicationEvents().get(0)
                instanceof EurekaInstanceCanceledEvent);

        // event details are correct
        EurekaInstanceCanceledEvent registeredEvent = (EurekaInstanceCanceledEvent)
                (eurekaEventListener.getApplicationEvents().get(0));
        assertEquals("my-app", registeredEvent.getAppName());
        assertEquals("appId", registeredEvent.getServerId());
        assertEquals(instanceRegistry, registeredEvent.getSource());
        assertFalse(registeredEvent.isReplication());

        // superclass method wrapper was successfully invoked
        verify(instanceRegistry).superInternalCancel("my-app", "appId", false);
        assertTrue(cancellationResult);
    }

    @Configuration
    @EnableAutoConfiguration
    @EnableEurekaServer
    protected static class Application {

        public static void main(String[] args) {
            new SpringApplicationBuilder(Application.class)
                    .properties("spring.application.name=eureka").run(args);
        }

        @Bean
        public Listener eurekaEventsListener() {
            return new Listener();
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

    private static class Listener implements ApplicationListener {

        private final List<ApplicationEvent> applicationEvents = new LinkedList<>();

        @Override
        public void onApplicationEvent(ApplicationEvent event) {
            if (event instanceof EurekaInstanceCanceledEvent ||
                    event instanceof EurekaInstanceRegisteredEvent) {
                applicationEvents.add(event);
            }
        }

        public List<ApplicationEvent> getApplicationEvents() {
            return applicationEvents;
        }
    }
}