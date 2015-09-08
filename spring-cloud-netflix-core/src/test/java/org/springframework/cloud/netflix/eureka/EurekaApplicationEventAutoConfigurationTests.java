/*
 * Copyright 2013-2014 the original author or authors.
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
 */

package org.springframework.cloud.netflix.eureka;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.test.ImportAutoConfiguration;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { EurekaClientMockConfig.class, EurekaApplicationListenerConfig.class })
@WebAppConfiguration
@TestPropertySource(properties = "eureka.remoteStatus.polling.interval=10")
public class EurekaApplicationEventAutoConfigurationTests {
    @Autowired
    EurekaClient eurekaClient;

    @Autowired
    EurekaStatusChangedListener listener;

    @Test
    public void changesToInstanceStatusInEurekaCauseApplicationEventsToBeFired() throws InterruptedException {
        when(eurekaClient.getInstanceRemoteStatus())
                .thenReturn(InstanceInfo.InstanceStatus.UP)
                .thenReturn(InstanceInfo.InstanceStatus.DOWN);

        listener.latch.await(50, TimeUnit.MILLISECONDS);
        assertEquals(2, listener.latch.getCount());
    }
}

@Configuration
class EurekaClientMockConfig {
    @Bean
    EurekaClient eurekaClient() {
        return mock(EurekaClient.class);
    }
}

@Component
@ImportAutoConfiguration(PropertyPlaceholderAutoConfiguration.class)
@Import(EurekaApplicationEventAutoConfiguration.class)
class EurekaApplicationListenerConfig {
    @Bean
    EurekaStatusChangedListener statusListener() {
        return new EurekaStatusChangedListener();
    }
}

class EurekaStatusChangedListener implements ApplicationListener<EurekaStatusChangedEvent> {
    CountDownLatch latch = new CountDownLatch(2);

    @Override
    public void onApplicationEvent(EurekaStatusChangedEvent eurekaStatusChangedEvent) {
        latch.countDown();
    }
}
