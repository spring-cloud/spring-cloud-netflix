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
 *
 */

package org.springframework.cloud.netflix.rx;

import java.lang.reflect.Field;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.context.request.async.DeferredResult;

import static org.springframework.util.ReflectionUtils.findField;
import static org.springframework.util.ReflectionUtils.getField;
import static org.springframework.util.ReflectionUtils.makeAccessible;

import rx.Single;

/**
 * @author Spencer Gibb
 */
public class SingleDeferredResultTests {

	private Object resultNone;
	private Field timeoutResultField;

	@Before
	public void init() {
		Field resultNoneField = findField(DeferredResult.class, "RESULT_NONE");
		makeAccessible(resultNoneField);
		resultNone = getField(resultNoneField, null);

		timeoutResultField = findField(DeferredResult.class, "timeoutResult");
		makeAccessible(timeoutResultField);
	}

	@Test
	public void testDefaultTimeoutResult() {
		Object timeoutResult = getField(timeoutResultField, new SingleDeferredResult<>(Single.just("")));

		Assertions.assertThat(timeoutResult).as("timeoutResult was not the default").isSameAs(resultNone);
	}

	@Test
	public void testDefaultTimeoutResultWithTimeout() {
		Object timeoutResult = getField(timeoutResultField, new SingleDeferredResult<>(1L, Single.just("")));

		Assertions.assertThat(timeoutResult).as("timeoutResult was not the default").isSameAs(resultNone);
	}

	@Test
	public void testCustomTimeoutResultWithTimeout() {
		Object customTimeoutResult = new Object();
		Object timeoutResult = getField(timeoutResultField, new SingleDeferredResult<>(1L, customTimeoutResult, Single.just("")));

		Assertions.assertThat(timeoutResult).as("timeoutResult was not the custom one").isSameAs(customTimeoutResult);
	}

}
