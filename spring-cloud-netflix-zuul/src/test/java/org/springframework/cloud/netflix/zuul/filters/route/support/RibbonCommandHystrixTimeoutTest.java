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

package org.springframework.cloud.netflix.zuul.filters.route.support;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.ribbon.support.RibbonCommandContext;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.route.apache.HttpClientRibbonCommand;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * @author Gang Li
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = RibbonCommandHystrixTimeoutTest.SimpleApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    value = {"hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds:60000"})
@DirtiesContext
public class RibbonCommandHystrixTimeoutTest {

    private ZuulProperties zuulProperties;

    @Before
    public void setUp()	{
        zuulProperties = new ZuulProperties();
    }

    @Test
    public void testHystrixTimeout() {
        RibbonCommandContext context = mock(RibbonCommandContext.class);
        HttpClientRibbonCommand command = new HttpClientRibbonCommand("cmd", null, context, zuulProperties);
        assertEquals(command.getProperties().executionTimeoutInMilliseconds().get().intValue(), 60000);
    }

    @Configuration
    @EnableAutoConfiguration
    @EnableZuulProxy
    public static class SimpleApplication {

    }

}
