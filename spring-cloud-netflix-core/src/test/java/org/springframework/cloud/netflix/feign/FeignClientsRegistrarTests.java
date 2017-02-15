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

package org.springframework.cloud.netflix.feign;

import java.util.Collections;

import org.junit.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Spencer Gibb
 */
public class FeignClientsRegistrarTests {

	@Test(expected = IllegalStateException.class)
	public void badNameHttpPrefix() {
		testGetName("http://bad_hostname");
	}


	@Test(expected = IllegalStateException.class)
	public void badNameHttpsPrefix() {
		testGetName("https://bad_hostname");
	}

	@Test(expected = IllegalStateException.class)
	public void badName() {
		testGetName("bad_hostname");
	}

	@Test(expected = IllegalStateException.class)
	public void badNameStartsWithHttp() {
		testGetName("http_bad_hostname");
	}

	@Test
	public void goodName() {
		String name = testGetName("good-name");
		assertThat("name was wrong", name, is("good-name"));
	}

	@Test
	public void goodNameHttpPrefix() {
		String name = testGetName("http://good-name");
		assertThat("name was wrong", name, is("http://good-name"));
	}

	@Test
	public void goodNameHttpsPrefix() {
		String name = testGetName("https://goodname");
		assertThat("name was wrong", name, is("https://goodname"));
	}

	private String testGetName(String name) {
		FeignClientsRegistrar registrar = new FeignClientsRegistrar();
		registrar.setEnvironment(new MockEnvironment());
		return registrar.getName(Collections.<String, Object>singletonMap("name", name));
	}
}
