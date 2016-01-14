/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.cloud.netflix.feign;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;

/**
 * @author Spencer Gibb
 */
public class FeignClientFactoryTests {

	@Test
	public void testChildContexts() {
		AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
		parent.refresh();
		FeignContext context = new FeignContext();
		context.setApplicationContext(parent);
		context.setConfigurations(Arrays.asList(getSpec("foo", FooConfig.class),
				getSpec("bar", BarConfig.class)));

		Foo foo = context.getInstance("foo", Foo.class);
		assertThat("foo was null", foo, is(notNullValue()));

		Bar bar = context.getInstance("bar", Bar.class);
		assertThat("bar was null", bar, is(notNullValue()));

		Bar foobar = context.getInstance("foo", Bar.class);
		assertThat("bar was not null", foobar, is(nullValue()));
	}

	private FeignClientSpecification getSpec(String name, Class<?> configClass) {
		return new FeignClientSpecification(name, new Class[]{configClass});
	}

	static class FooConfig {
		@Bean
		Foo foo() {
			return new Foo();
		}

	}
	static class Foo{}

	static class BarConfig {
		@Bean
		Bar bar() {
			return new Bar();
		}
	}
	static class Bar{}
}
