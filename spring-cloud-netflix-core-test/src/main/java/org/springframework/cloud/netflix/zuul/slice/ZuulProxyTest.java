/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.cloud.netflix.zuul.slice;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
import org.springframework.boot.test.autoconfigure.filter.TypeExcludeFilters;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.test.context.BootstrapWith;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that can be used in combination with {@code @RunWith(SpringRunner.class)}
 * for a testing zuul related configuration in isolation.
 * 
 * <p>
 * Using this annotation will disable full auto-configuration and instead apply only
 * auto-configuration's relevant to Zuul Tests and will scan in components that are
 * relevant to Zuul - {@code com.netflix.zuul.ZuulFilter}
 * <p>
 * This annotation also starts up WireMock using
 * {@link AutoConfigureWireMock @AutoConfigureWireMock} which can be used for simulating
 * the behavior of proxied remote service
 * <p>
 * 
 * @see AutoConfigureWireMock
 * 
 * @author Biju Kunjummen
 * 
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@BootstrapWith(ZuulTestContextBootstrapper.class)
@OverrideAutoConfiguration(enabled = false)
@TypeExcludeFilters(ZuulTypeExcludeFilter.class)
@AutoConfigureWebMvc
@AutoConfigureMockMvc(secure = false)
@EnableZuulProxy
@AutoConfigureZuulProxy
@AutoConfigureWireMock(port = 0)
@ImportAutoConfiguration
public @interface ZuulProxyTest {
	String[] properties() default {};
}
