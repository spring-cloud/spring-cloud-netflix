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
 */

package org.springframework.cloud.netflix.feign.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.CollationElementIterator;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeTrue;

import feign.MethodMetadata;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * @author chadjaros
 */
public class SpringMvcContractTests {
	private static final Class<?> EXECUTABLE_TYPE;

	static {
		Class<?> executableType;
		try {
			executableType = Class.forName("java.lang.reflect.Executable");
		}
		catch (ClassNotFoundException ex) {
			executableType = null;
		}
		EXECUTABLE_TYPE = executableType;
	}

	private SpringMvcContract contract;

	@Before
	public void setup() {
		this.contract = new SpringMvcContract();
	}

	@Test
	public void testProcessAnnotationOnMethod_Simple() throws Exception {
		Method method = TestTemplate_Simple.class.getDeclaredMethod("getTest",
				String.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertEquals("/test/{id}", data.template().url());
		assertEquals("GET", data.template().method());
		assertEquals(MediaType.APPLICATION_JSON_VALUE,
				data.template().headers().get("Accept").iterator().next());
	}

	@Test
	public void testProcessAnnotations_Simple() throws Exception {
		Method method = TestTemplate_Simple.class.getDeclaredMethod("getTest",
				String.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertEquals("/test/{id}", data.template().url());
		assertEquals("GET", data.template().method());
		assertEquals(MediaType.APPLICATION_JSON_VALUE,
				data.template().headers().get("Accept").iterator().next());

		assertEquals("id", data.indexToName().get(0).iterator().next());
	}

	@Test
	public void testProcessAnnotations_SimpleGetMapping() throws Exception {
		Method method = TestTemplate_Simple.class.getDeclaredMethod("getMappingTest",
				String.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertEquals("/test/{id}", data.template().url());
		assertEquals("GET", data.template().method());
		assertEquals(MediaType.APPLICATION_JSON_VALUE,
				data.template().headers().get("Accept").iterator().next());

		assertEquals("id", data.indexToName().get(0).iterator().next());
	}

	@Test
	public void testProcessAnnotations_Class_AnnotationsGetSpecificTest()
			throws Exception {
		Method method = TestTemplate_Class_Annotations.class
				.getDeclaredMethod("getSpecificTest", String.class, String.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertEquals("/prepend/{classId}/test/{testId}", data.template().url());
		assertEquals("GET", data.template().method());

		assertEquals("classId", data.indexToName().get(0).iterator().next());
		assertEquals("testId", data.indexToName().get(1).iterator().next());
	}

	@Test
	public void testProcessAnnotations_Class_AnnotationsGetAllTests() throws Exception {
		Method method = TestTemplate_Class_Annotations.class
				.getDeclaredMethod("getAllTests", String.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertEquals("/prepend/{classId}", data.template().url());
		assertEquals("GET", data.template().method());

		assertEquals("classId", data.indexToName().get(0).iterator().next());
	}

	@Test
	public void testProcessAnnotations_ExtendedInterface() throws Exception {
		Method extendedMethod = TestTemplate_Extended.class.getMethod("getAllTests",
				String.class);
		MethodMetadata extendedData = this.contract.parseAndValidateMetadata(
				extendedMethod.getDeclaringClass(), extendedMethod);

		Method method = TestTemplate_Class_Annotations.class
				.getDeclaredMethod("getAllTests", String.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertEquals(extendedData.template().url(), data.template().url());
		assertEquals(extendedData.template().method(), data.template().method());

		assertEquals(data.indexToName().get(0).iterator().next(),
				data.indexToName().get(0).iterator().next());
	}

	@Test
	public void testProcessAnnotations_SimplePost() throws Exception {
		Method method = TestTemplate_Simple.class.getDeclaredMethod("postTest",
				TestObject.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertEquals("", data.template().url());
		assertEquals("POST", data.template().method());
		assertEquals(MediaType.APPLICATION_JSON_VALUE,
				data.template().headers().get("Accept").iterator().next());

	}

	@Test
	public void testProcessAnnotations_SimplePostMapping() throws Exception {
		Method method = TestTemplate_Simple.class.getDeclaredMethod("postMappingTest",
				TestObject.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertEquals("", data.template().url());
		assertEquals("POST", data.template().method());
		assertEquals(MediaType.APPLICATION_JSON_VALUE,
				data.template().headers().get("Accept").iterator().next());

	}

	@Test
	public void testProcessAnnotationsOnMethod_Advanced() throws Exception {
		Method method = TestTemplate_Advanced.class.getDeclaredMethod("getTest",
				String.class, String.class, Integer.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertEquals("/advanced/test/{id}", data.template().url());
		assertEquals("PUT", data.template().method());
		assertEquals(MediaType.APPLICATION_JSON_VALUE,
				data.template().headers().get("Accept").iterator().next());
	}

	@Test
	public void testProcessAnnotationsOnMethod_Advanced_UnknownAnnotation()
			throws Exception {
		Method method = TestTemplate_Advanced.class.getDeclaredMethod("getTest",
				String.class, String.class, Integer.class);
		this.contract.parseAndValidateMetadata(method.getDeclaringClass(), method);

		// Don't throw an exception and this passes
	}

	@Test
	public void testProcessAnnotations_Advanced() throws Exception {
		Method method = TestTemplate_Advanced.class.getDeclaredMethod("getTest",
				String.class, String.class, Integer.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertEquals("/advanced/test/{id}", data.template().url());
		assertEquals("PUT", data.template().method());
		assertEquals(MediaType.APPLICATION_JSON_VALUE,
				data.template().headers().get("Accept").iterator().next());

		assertEquals("Authorization", data.indexToName().get(0).iterator().next());
		assertEquals("id", data.indexToName().get(1).iterator().next());
		assertEquals("amount", data.indexToName().get(2).iterator().next());
		assertNotNull(data.indexToExpander().get(2));

		assertEquals("{Authorization}",
				data.template().headers().get("Authorization").iterator().next());
		assertEquals("{amount}",
				data.template().queries().get("amount").iterator().next());
	}

	@Test
	public void testProcessAnnotations_Aliased() throws Exception {
		Method method = TestTemplate_Advanced.class.getDeclaredMethod("getTest2",
				String.class, Integer.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertEquals("/advanced/test2", data.template().url());
		assertEquals("PUT", data.template().method());
		assertEquals(MediaType.APPLICATION_JSON_VALUE,
				data.template().headers().get("Accept").iterator().next());

		assertEquals("Authorization", data.indexToName().get(0).iterator().next());
		assertEquals("amount", data.indexToName().get(1).iterator().next());

		assertEquals("{Authorization}",
				data.template().headers().get("Authorization").iterator().next());
		assertEquals("{amount}",
				data.template().queries().get("amount").iterator().next());
	}

	@Test
	public void testProcessAnnotations_Advanced2() throws Exception {
		Method method = TestTemplate_Advanced.class.getDeclaredMethod("getTest");
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertEquals("/advanced", data.template().url());
		assertEquals("GET", data.template().method());
		assertEquals(MediaType.APPLICATION_JSON_VALUE,
				data.template().headers().get("Accept").iterator().next());
	}

	@Test
	public void testProcessAnnotations_Advanced3() throws Exception {
		Method method = TestTemplate_Simple.class.getDeclaredMethod("getTest");
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertEquals("", data.template().url());
		assertEquals("GET", data.template().method());
		assertEquals(MediaType.APPLICATION_JSON_VALUE,
				data.template().headers().get("Accept").iterator().next());
	}

	@Test
	public void testProcessAnnotations_ListParams() throws Exception {
		Method method = TestTemplate_ListParams.class.getDeclaredMethod("getTest",
				List.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertEquals("/test", data.template().url());
		assertEquals("GET", data.template().method());
		assertEquals("[{id}]", data.template().queries().get("id").toString());
		assertNotNull(data.indexToExpander().get(0));
	}

	@Test
	public void testProcessAnnotations_ListParamsWithoutName() throws Exception {
		Method method = TestTemplate_ListParamsWithoutName.class.getDeclaredMethod("getTest",
				List.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertEquals("/test", data.template().url());
		assertEquals("GET", data.template().method());
		assertEquals("[{id}]", data.template().queries().get("id").toString());
		assertNotNull(data.indexToExpander().get(0));
	}

	@Test
	public void testProcessAnnotations_MapParams() throws Exception {
		Method method = TestTemplate_MapParams.class.getDeclaredMethod("getTest",
				Map.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertEquals("/test", data.template().url());
		assertEquals("GET", data.template().method());
		assertNotNull(data.queryMapIndex());
		assertEquals(0, data.queryMapIndex().intValue());
	}

	@Test
	public void testProcessHeaders() throws Exception {
		Method method = TestTemplate_Headers.class.getDeclaredMethod("getTest",
				String.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertEquals("/test/{id}", data.template().url());
		assertEquals("GET", data.template().method());
		assertEquals("bar", data.template().headers().get("X-Foo").iterator().next());
	}

	@Test
	public void testProcessHeadersWithoutValues() throws Exception {
		Method method = TestTemplate_HeadersWithoutValues.class.getDeclaredMethod("getTest",
				String.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertEquals("/test/{id}", data.template().url());
		assertEquals("GET", data.template().method());
		assertEquals(true, data.template().headers().isEmpty());
	}

	@Test
	public void testProcessAnnotations_Fallback() throws Exception {
		Method method = TestTemplate_Advanced.class.getDeclaredMethod("getTestFallback",
				String.class, String.class, Integer.class);

		assumeTrue("does not have java 8 parameter names", hasJava8ParameterNames(method));

		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertEquals("/advanced/testfallback/{id}", data.template().url());
		assertEquals("PUT", data.template().method());
		assertEquals(MediaType.APPLICATION_JSON_VALUE,
				data.template().headers().get("Accept").iterator().next());

		assertEquals("Authorization", data.indexToName().get(0).iterator().next());
		assertEquals("id", data.indexToName().get(1).iterator().next());
		assertEquals("amount", data.indexToName().get(2).iterator().next());

		assertEquals("{Authorization}",
				data.template().headers().get("Authorization").iterator().next());
		assertEquals("{amount}",
				data.template().queries().get("amount").iterator().next());
	}

	/**
	 * For abstract (e.g. interface) methods, only Java 8 Parameter names (compiler arg
	 * -parameters) can supply parameter names; bytecode-based strategies use local
	 * variable declarations, of which there are none for abstract methods.
	 * @param m
	 * @return whether a parameter name was found
	 * @throws IllegalArgumentException if method has no parameters
	 */
	private static boolean hasJava8ParameterNames(Method m) {
		org.springframework.util.Assert.isTrue(m.getParameterTypes().length > 0,
				"method has no parameters");
		if (EXECUTABLE_TYPE != null) {
			Method getParameters = ReflectionUtils.findMethod(EXECUTABLE_TYPE,
					"getParameters");
			try {
				Object[] parameters = (Object[]) getParameters.invoke(m);
				Method isNamePresent = ReflectionUtils
						.findMethod(parameters[0].getClass(), "isNamePresent");
				return Boolean.TRUE.equals(isNamePresent.invoke(parameters[0]));
			}
			catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException ex) {
			}
		}
		return false;
	}

	@Test
	public void testProcessHeaderMap() throws Exception {
		Method method = TestTemplate_HeaderMap.class.getDeclaredMethod("headerMap",
				MultiValueMap.class, String.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertEquals("/headerMap", data.template().url());
		assertEquals("GET", data.template().method());
		assertEquals(0, data.headerMapIndex().intValue());
		Map<String, Collection<String>> headers = data.template().headers();
		assertEquals("{aHeader}", headers.get("aHeader").iterator().next());
	}

	@Test(expected = IllegalStateException.class)
	public void testProcessHeaderMapMoreThanOnce() throws Exception {
		Method method = TestTemplate_HeaderMap.class.getDeclaredMethod(
				"headerMapMoreThanOnce", MultiValueMap.class, MultiValueMap.class);
		this.contract.parseAndValidateMetadata(method.getDeclaringClass(), method);
	}

	@Test
	public void testProcessQueryMap() throws Exception {
		Method method = TestTemplate_QueryMap.class.getDeclaredMethod("queryMap",
				MultiValueMap.class, String.class);
		MethodMetadata data = this.contract
				.parseAndValidateMetadata(method.getDeclaringClass(), method);

		assertEquals("/queryMap", data.template().url());
		assertEquals("GET", data.template().method());
		assertEquals(0, data.queryMapIndex().intValue());
		Map<String, Collection<String>> params = data.template().queries();
		assertEquals("{aParam}", params.get("aParam").iterator().next());
	}

	@Test(expected = IllegalStateException.class)
	public void testProcessQueryMapMoreThanOnce() throws Exception {
		Method method = TestTemplate_QueryMap.class.getDeclaredMethod(
				"queryMapMoreThanOnce", MultiValueMap.class, MultiValueMap.class);
		this.contract.parseAndValidateMetadata(method.getDeclaringClass(), method);
	}

	public interface TestTemplate_Simple {
		@RequestMapping(value = "/test/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
		ResponseEntity<TestObject> getTest(@PathVariable("id") String id);

		@RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
		TestObject getTest();

		@GetMapping(value = "/test/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
		ResponseEntity<TestObject> getMappingTest(@PathVariable("id") String id);

		@RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
		TestObject postTest(@RequestBody TestObject object);

		@PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
		TestObject postMappingTest(@RequestBody TestObject object);
	}

	@RequestMapping("/prepend/{classId}")
	public interface TestTemplate_Class_Annotations {
		@RequestMapping(value = "/test/{testId}", method = RequestMethod.GET)
		TestObject getSpecificTest(@PathVariable("classId") String classId,
				@PathVariable("testId") String testId);

		@RequestMapping(method = RequestMethod.GET)
		TestObject getAllTests(@PathVariable("classId") String classId);
	}

	public interface TestTemplate_Extended extends TestTemplate_Class_Annotations {

	}

	public interface TestTemplate_Headers {
		@RequestMapping(value = "/test/{id}", method = RequestMethod.GET, headers = "X-Foo=bar")
		ResponseEntity<TestObject> getTest(@PathVariable("id") String id);
	}

	public interface TestTemplate_HeadersWithoutValues {
		@RequestMapping(value = "/test/{id}", method = RequestMethod.GET, headers = { "X-Foo", "!X-Bar", "X-Baz!=fooBar" })
		ResponseEntity<TestObject> getTest(@PathVariable("id") String id);
	}

	public interface TestTemplate_ListParams {
		@RequestMapping(value = "/test", method = RequestMethod.GET)
		ResponseEntity<TestObject> getTest(@RequestParam("id") List<String> id);
	}

	public interface TestTemplate_ListParamsWithoutName {
		@RequestMapping(value = "/test", method = RequestMethod.GET)
		ResponseEntity<TestObject> getTest(@RequestParam List<String> id);
	}

	public interface TestTemplate_MapParams {
		@RequestMapping(value = "/test", method = RequestMethod.GET)
		ResponseEntity<TestObject> getTest(@RequestParam Map<String, String> params);
	}

	public interface TestTemplate_HeaderMap {
		@RequestMapping(path = "/headerMap")
		String headerMap(
				@RequestHeader MultiValueMap<String, String> headerMap,
				@RequestHeader(name = "aHeader") String aHeader);

		@RequestMapping(path = "/headerMapMoreThanOnce")
		String headerMapMoreThanOnce(
				@RequestHeader MultiValueMap<String, String> headerMap1,
				@RequestHeader MultiValueMap<String, String> headerMap2);
	}

	public interface TestTemplate_QueryMap {
		@RequestMapping(path = "/queryMap")
		String queryMap(
				@RequestParam MultiValueMap<String, String> queryMap,
				@RequestParam(name = "aParam") String aParam);

		@RequestMapping(path = "/queryMapMoreThanOnce")
		String queryMapMoreThanOnce(
				@RequestParam MultiValueMap<String, String> queryMap1,
				@RequestParam MultiValueMap<String, String> queryMap2);
	}

	@JsonAutoDetect
	@RequestMapping("/advanced")
	public interface TestTemplate_Advanced {

		@ExceptionHandler
		@RequestMapping(path = "/test/{id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
		ResponseEntity<TestObject> getTest(@RequestHeader("Authorization") String auth,
				@PathVariable("id") String id, @RequestParam("amount") Integer amount);
 
		@RequestMapping(path = "/test2", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
		ResponseEntity<TestObject> getTest2(
				@RequestHeader(name = "Authorization") String auth,
				@RequestParam(name = "amount") Integer amount);

		@ExceptionHandler
		@RequestMapping(path = "/testfallback/{id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
		ResponseEntity<TestObject> getTestFallback(@RequestHeader String Authorization,
				@PathVariable String id, @RequestParam Integer amount);

		@RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
		TestObject getTest();
	}

	@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
	public class TestObject {

		public String something;
		public Double number;

		public TestObject() {
		}

		public TestObject(String something, Double number) {
			this.something = something;
			this.number = number;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			TestObject that = (TestObject) o;

			if (this.number != null ? !this.number.equals(that.number)
					: that.number != null) {
				return false;
			}
			if (this.something != null ? !this.something.equals(that.something)
					: that.something != null) {
				return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			int result = (this.something != null ? this.something.hashCode() : 0);
			result = 31 * result + (this.number != null ? this.number.hashCode() : 0);
			return result;
		}

		@Override
		public String toString() {
			return new StringBuilder("TestObject{")
					.append("something='").append(something).append("', ")
					.append("number=").append(number)
					.append("}").toString();
		}
	}
}
