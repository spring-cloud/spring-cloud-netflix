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

package org.springframework.cloud.netflix.ribbon;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.ServerListFilter;

/**
 * Declarative configuration for a ribbon client. Add this annotation to any
 * <code>@Configuration</code> and then inject a {@link SpringClientFactory} to access the
 * client that is created.
 *
 * @author Dave Syer
 */
@Configuration
@Import(RibbonClientConfigurationRegistrar.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RibbonClient {

	/**
	 * Synonym for name (the name of the client)
	 *
	 * @see #name()
	 */
	String value() default "";

	/**
	 * The name of the ribbon client, uniquely identifying a set of client resources,
	 * including a load balancer.
	 */
	String name() default "";

	/**
	 * A custom <code>@Configuration</code> for the ribbon client. Can contain override
	 * <code>@Bean</code> definition for the pieces that make up the client, for instance
	 * {@link ILoadBalancer}, {@link ServerListFilter}, {@link IRule}.
	 *
	 * @see RibbonClientConfiguration for the defaults
	 */
	Class<?>[] configuration() default {};

}
