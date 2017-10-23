/*
 * Copyright 2013-2017 the original author or authors.
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

package org.springframework.cloud.netflix.test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assume.assumeThat;

public class TestUtils {
	public static void assumeTestIgnored(Class clazz) {
		assumeTestIgnored(clazz.getSimpleName());
	}

	public static void assumeTestIgnored(String name) {
		assumeThat("Test ignored",
				System.getenv("SPRING_CLOUD_NETFLIX_IGNORE_TESTS"),
				not(containsString(name)));
	}
}
