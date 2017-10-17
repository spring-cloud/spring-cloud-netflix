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

package org.springframework.cloud.netflix.feign.valid;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.cloud.netflix.feign.FeignFormatterRegistrar;
import org.springframework.cloud.netflix.feign.ribbon.LoadBalancerFeignClient;
import org.springframework.cloud.netflix.feign.support.FallbackCommand;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;

import feign.Client;
import feign.Feign;
import feign.Logger;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.Target;
import feign.hystrix.FallbackFactory;
import feign.hystrix.SetterFactory;
import rx.Observable;
import rx.Single;

/**
 * @author Spencer Gibb
 * @author Jakub Narloch
 * @author Erik Kringen
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = FeignClientTests.Application.class, webEnvironment = WebEnvironment.RANDOM_PORT, value = {
		"spring.application.name=feignclienttest",
		"logging.level.org.springframework.cloud.netflix.feign.valid=DEBUG",
		"feign.httpclient.enabled=false", "feign.okhttp.enabled=false",
		"feign.hystrix.enabled=true"})
@DirtiesContext
public class FeignClientTests {

	public static final String HELLO_WORLD_1 = "hello world 1";
	public static final String OI_TERRA_2 = "oi terra 2";
	public static final String MYHEADER1 = "myheader1";
	public static final String MYHEADER2 = "myheader2";

	@Value("${local.server.port}")
	private int port = 0;

	@Autowired
	private TestClient testClient;

	@Autowired
	private TestClientServiceId testClientServiceId;

	@Autowired
	private DecodingTestClient decodingTestClient;

	@Autowired
	private Client feignClient;

	@Autowired
	HystrixClient hystrixClient;

	@Autowired
	private HystrixClientWithFallBackFactory hystrixClientWithFallBackFactory;

	@Autowired
	@Qualifier("localapp3FeignClient")
	HystrixClient namedHystrixClient;

	@Autowired
	HystrixSetterFactoryClient hystrixSetterFactoryClient;

	protected enum Arg {
		A, B;

		@Override
		public String toString() {
			return name().toLowerCase(Locale.ENGLISH);
		}
	}

	protected static class OtherArg {
		public final String value;

		public OtherArg(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}
	}

	@FeignClient(name = "localapp", configuration = TestClientConfig.class)
	protected interface TestClient {
		@RequestMapping(method = RequestMethod.GET, path = "/hello")
		Hello getHello();

		@RequestMapping(method = RequestMethod.GET, path = "${feignClient.methodLevelRequestMappingPath}")
		Hello getHelloUsingPropertyPlaceHolder();

		@RequestMapping(method = RequestMethod.GET, path = "/hello")
		Single<Hello> getHelloSingle();

		@RequestMapping(method = RequestMethod.GET, path = "/hellos")
		List<Hello> getHellos();

		@RequestMapping(method = RequestMethod.GET, path = "/hellostrings")
		List<String> getHelloStrings();

		@RequestMapping(method = RequestMethod.GET, path = "/helloheaders")
		List<String> getHelloHeaders();

		@RequestMapping(method = RequestMethod.GET, path = "/helloheadersplaceholders", headers = "myPlaceholderHeader=${feignClient.myPlaceholderHeader}")
		String getHelloHeadersPlaceholders();

		@RequestMapping(method = RequestMethod.GET, path = "/helloparams")
		List<String> getParams(@RequestParam("params") List<String> params);

		@RequestMapping(method = RequestMethod.GET, path = "/hellos")
		HystrixCommand<List<Hello>> getHellosHystrix();

		@RequestMapping(method = RequestMethod.GET, path = "/noContent")
		ResponseEntity<Void> noContent();

		@RequestMapping(method = RequestMethod.HEAD, path = "/head")
		ResponseEntity<Void> head();

		@RequestMapping(method = RequestMethod.GET, path = "/hello")
		HttpEntity<Hello> getHelloEntity();

		@RequestMapping(method = RequestMethod.POST, consumes = "application/vnd.io.spring.cloud.test.v1+json", produces = "application/vnd.io.spring.cloud.test.v1+json", path = "/complex")
		String moreComplexContentType(String body);

		@RequestMapping(method = RequestMethod.GET, path = "/tostring")
		String getToString(@RequestParam("arg") Arg arg);

		@RequestMapping(method = RequestMethod.GET, path = "/tostring2")
		String getToString(@RequestParam("arg") OtherArg arg);

		@RequestMapping(method = RequestMethod.GET, path = "/tostringcollection")
		Collection<String> getToString(@RequestParam("arg") Collection<OtherArg> args);
	}

	public static class TestClientConfig {

		@Bean
		public RequestInterceptor interceptor1() {
			return new RequestInterceptor() {
				@Override
				public void apply(RequestTemplate template) {
					template.header(MYHEADER1, "myheader1value");
				}
			};
		}

		@Bean
		public RequestInterceptor interceptor2() {
			return new RequestInterceptor() {
				@Override
				public void apply(RequestTemplate template) {
					template.header(MYHEADER2, "myheader2value");
				}
			};
		}
	}

	@FeignClient(name = "localapp1")
	protected interface TestClientServiceId {
		@RequestMapping(method = RequestMethod.GET, path = "/hello")
		Hello getHello();
	}

	@FeignClient(name = "localapp2", decode404 = true)
	protected interface DecodingTestClient {
		@RequestMapping(method = RequestMethod.GET, path = "/notFound")
		ResponseEntity<String> notFound();
	}

	@FeignClient(name = "localapp3", fallback = HystrixClientFallback.class)
	protected interface HystrixClient {
		@RequestMapping(method = RequestMethod.GET, path = "/fail")
		Single<Hello> failSingle();

		@RequestMapping(method = RequestMethod.GET, path = "/fail")
		Hello fail();

		@RequestMapping(method = RequestMethod.GET, path = "/fail")
		HystrixCommand<Hello> failCommand();

		@RequestMapping(method = RequestMethod.GET, path = "/fail")
		Observable<Hello> failObservable();

		@RequestMapping(method = RequestMethod.GET, path = "/fail")
		Future<Hello> failFuture();
	}

	@FeignClient(name = "localapp4", fallbackFactory = HystrixClientFallbackFactory.class)
	protected interface HystrixClientWithFallBackFactory {

		@RequestMapping(method = RequestMethod.GET, path = "/fail")
		Hello fail();
	}

	static class HystrixClientFallbackFactory implements FallbackFactory<HystrixClientWithFallBackFactory> {

		@Override
		public HystrixClientWithFallBackFactory create(final Throwable cause) {
			return new HystrixClientWithFallBackFactory() {
				@Override
				public Hello fail() {
					assertNotNull("Cause was null", cause);
					return new Hello("Hello from the fallback side: " + cause.getMessage());
				}
			};
		}
	}

	static class HystrixClientFallback implements HystrixClient {
		@Override
		public Hello fail() {
			return new Hello("fallback");
		}

		@Override
		public Single<Hello> failSingle() {
			return Single.just(new Hello("fallbacksingle"));
		}

		@Override
		public HystrixCommand<Hello> failCommand() {
			return new FallbackCommand<>(new Hello("fallbackcommand"));
		}

		@Override
		public Observable<Hello> failObservable() {
			return Observable.just(new Hello("fallbackobservable"));
		}

		@Override
		public Future<Hello> failFuture() {
			return new FallbackCommand<>(new Hello("fallbackfuture")).queue();
		}
	}

	@FeignClient(name = "localapp5", configuration = TestHystrixSetterFactoryClientConfig.class)
	protected interface HystrixSetterFactoryClient {
		@RequestMapping(method = RequestMethod.GET, path = "/hellos")
		HystrixCommand<List<Hello>> getHellosHystrix();
	}

	public static class TestHystrixSetterFactoryClientConfig {
		public static final String SETTER_PREFIX = "SETTER-";
		@Bean
		public SetterFactory commandKeyIsRequestLineSetterFactory() {
			return new SetterFactory() {
				@Override public HystrixCommand.Setter create(Target<?> target,
					Method method) {
					String groupKey = SETTER_PREFIX + target.name();
					RequestMapping requestMapping = method
						.getAnnotation(RequestMapping.class);
					String commandKey =
						SETTER_PREFIX + requestMapping.method()[0] + " " + requestMapping
							.path()[0];
					return HystrixCommand.Setter
						.withGroupKey(HystrixCommandGroupKey.Factory.asKey(groupKey))
						.andCommandKey(HystrixCommandKey.Factory.asKey(commandKey));
				}
			};
		}
	}

	@Configuration
	@EnableAutoConfiguration
	@RestController
	@EnableFeignClients(clients = { TestClientServiceId.class, TestClient.class,
		DecodingTestClient.class, HystrixClient.class, HystrixClientWithFallBackFactory.class,
		HystrixSetterFactoryClient.class},
		defaultConfiguration = TestDefaultFeignConfig.class)
	@RibbonClients({
		@RibbonClient(name = "localapp", configuration = LocalRibbonClientConfiguration.class),
		@RibbonClient(name = "localapp1", configuration = LocalRibbonClientConfiguration.class),
		@RibbonClient(name = "localapp2", configuration = LocalRibbonClientConfiguration.class),
		@RibbonClient(name = "localapp3", configuration = LocalRibbonClientConfiguration.class),
		@RibbonClient(name = "localapp4", configuration = LocalRibbonClientConfiguration.class),
		@RibbonClient(name = "localapp5", configuration = LocalRibbonClientConfiguration.class)
	})
	protected static class Application {

		// needs to be in parent context to test multiple HystrixClient beans
		@Bean
		public HystrixClientFallback hystrixClientFallback() {
			return new HystrixClientFallback();
		}

		@Bean
		public HystrixClientFallbackFactory hystrixClientFallbackFactory() {
			return new HystrixClientFallbackFactory();
		}

		@Bean
		FeignFormatterRegistrar feignFormatterRegistrar() {
			return new FeignFormatterRegistrar() {

				@Override
				public void registerFormatters(FormatterRegistry registry) {
					registry.addFormatter(new Formatter<OtherArg>() {

						@Override
						public String print(OtherArg object, Locale locale) {
							if("foo".equals(object.value)) {
								return "bar";
							}
							return object.value;
						}

						@Override
						public OtherArg parse(String text, Locale locale)
								throws ParseException {
							return new OtherArg(text);
						}
					});
				}
			};
		}

		@RequestMapping(method = RequestMethod.GET, path = "/hello")
		public Hello getHello() {
			return new Hello(HELLO_WORLD_1);
		}

		@RequestMapping(method = RequestMethod.GET, path = "/hello2")
		public Hello getHello2() {
			return new Hello(OI_TERRA_2);
		}

		@RequestMapping(method = RequestMethod.GET, path = "/hellos")
		public List<Hello> getHellos() {
			ArrayList<Hello> hellos = getHelloList();
			return hellos;
		}

		@RequestMapping(method = RequestMethod.GET, path = "/hellostrings")
		public List<String> getHelloStrings() {
			ArrayList<String> hellos = new ArrayList<>();
			hellos.add(HELLO_WORLD_1);
			hellos.add(OI_TERRA_2);
			return hellos;
		}

		@RequestMapping(method = RequestMethod.GET, path = "/helloheaders")
		public List<String> getHelloHeaders(@RequestHeader(MYHEADER1) String myheader1,
				@RequestHeader(MYHEADER2) String myheader2) {
			ArrayList<String> headers = new ArrayList<>();
			headers.add(myheader1);
			headers.add(myheader2);
			return headers;
		}

		@RequestMapping(method = RequestMethod.GET, path = "/helloheadersplaceholders")
		public String getHelloHeadersPlaceholders(
				@RequestHeader("myPlaceholderHeader") String myPlaceholderHeader) {
			return myPlaceholderHeader;
		}

		@RequestMapping(method = RequestMethod.GET, path = "/helloparams")
		public List<String> getParams(@RequestParam("params") List<String> params) {
			return params;
		}

		@RequestMapping(method = RequestMethod.GET, path = "/noContent")
		ResponseEntity<Void> noContent() {
			return ResponseEntity.noContent().build();
		}

		@RequestMapping(method = RequestMethod.HEAD, path = "/head")
		ResponseEntity<Void> head() {
			return ResponseEntity.ok().build();
		}

		@RequestMapping(method = RequestMethod.GET, path = "/fail")
		String fail() {
			throw new RuntimeException("always fails");
		}

		@RequestMapping(method = RequestMethod.GET, path = "/notFound")
		ResponseEntity<String> notFound() {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body((String) null);
		}

		@RequestMapping(method = RequestMethod.POST, consumes = "application/vnd.io.spring.cloud.test.v1+json", produces = "application/vnd.io.spring.cloud.test.v1+json", path = "/complex")
		String complex(@RequestBody String body, @RequestHeader("Content-Length") int contentLength) {
			if (contentLength <= 0) {
				throw new IllegalArgumentException("Invalid Content-Length "+ contentLength);
			}
			return body;
		}

		@RequestMapping(method = RequestMethod.GET, path = "/tostring")
		String getToString(@RequestParam("arg") Arg arg) {
			return arg.toString();
		}

		@RequestMapping(method = RequestMethod.GET, path = "/tostring2")
		String getToString(@RequestParam("arg") OtherArg arg) {
			return arg.value;
		}

		@RequestMapping(method = RequestMethod.GET, path = "/tostringcollection")
		Collection<String> getToString(@RequestParam("arg") Collection<OtherArg> args) {
			List<String> result = new ArrayList<>();
			for(OtherArg arg : args) {
				result.add(arg.value);
			}
			return result;
		}

		public static void main(String[] args) {
			new SpringApplicationBuilder(Application.class)
					.properties("spring.application.name=feignclienttest",
							"management.contextPath=/admin")
					.run(args);
		}
	}

	private static ArrayList<Hello> getHelloList() {
		ArrayList<Hello> hellos = new ArrayList<>();
		hellos.add(new Hello(HELLO_WORLD_1));
		hellos.add(new Hello(OI_TERRA_2));
		return hellos;
	}

	@Test
	public void testClient() {
		assertNotNull("testClient was null", this.testClient);
		assertTrue("testClient is not a java Proxy",
				Proxy.isProxyClass(this.testClient.getClass()));
		InvocationHandler invocationHandler = Proxy.getInvocationHandler(this.testClient);
		assertNotNull("invocationHandler was null", invocationHandler);
	}

	@Test
	public void testRequestMappingClassLevelPropertyReplacement() {
		Hello hello = this.testClient.getHelloUsingPropertyPlaceHolder();
		assertNotNull("hello was null", hello);
		assertEquals("first hello didn't match", new Hello(OI_TERRA_2), hello);
	}

	@Test
	public void testSimpleType() {
		Hello hello = this.testClient.getHello();
		assertNotNull("hello was null", hello);
		assertEquals("first hello didn't match", new Hello(HELLO_WORLD_1), hello);
	}

	@Test
	public void testGenericType() {
		List<Hello> hellos = this.testClient.getHellos();
		assertNotNull("hellos was null", hellos);
		assertEquals("hellos didn't match", hellos, getHelloList());
	}

	@Test
	public void testRequestInterceptors() {
		List<String> headers = this.testClient.getHelloHeaders();
		assertNotNull("headers was null", headers);
		assertTrue("headers didn't contain myheader1value",
				headers.contains("myheader1value"));
		assertTrue("headers didn't contain myheader2value",
				headers.contains("myheader2value"));
	}

	@Test
	public void testHeaderPlaceholders() {
		String header = this.testClient.getHelloHeadersPlaceholders();
		assertNotNull("header was null", header);
		assertEquals("header was wrong", "myPlaceholderHeaderValue", header);
	}

	@Test
	public void testFeignClientType() throws IllegalAccessException {
		assertThat(this.feignClient, is(instanceOf(LoadBalancerFeignClient.class)));
		LoadBalancerFeignClient client = (LoadBalancerFeignClient) this.feignClient;
		Client delegate = client.getDelegate();
		assertThat(delegate, is(instanceOf(feign.Client.Default.class)));
	}

	@Test
	public void testServiceId() {
		assertNotNull("testClientServiceId was null", this.testClientServiceId);
		final Hello hello = this.testClientServiceId.getHello();
		assertNotNull("The hello response was null", hello);
		assertEquals("first hello didn't match", new Hello(HELLO_WORLD_1), hello);
	}

	@Test
	public void testParams() {
		List<String> list = Arrays.asList("a", "1", "test");
		List<String> params = this.testClient.getParams(list);
		assertNotNull("params was null", params);
		assertEquals("params size was wrong", list.size(), params.size());
	}

	@Test
	public void testHystrixCommand() throws NoSuchMethodException {
		HystrixCommand<List<Hello>> command = this.testClient.getHellosHystrix();
		assertNotNull("command was null", command);
		assertEquals(
			"Hystrix command group name should match the name of the feign client",
			"localapp", command.getCommandGroup().name());
		String configKey = Feign.configKey(TestClient.class,
			TestClient.class.getMethod("getHellosHystrix", (Class<?>[]) null));
		assertEquals("Hystrix command key name should match the feign config key",
			configKey, command.getCommandKey().name());
		List<Hello> hellos = command.execute();
		assertNotNull("hellos was null", hellos);
		assertEquals("hellos didn't match", hellos, getHelloList());
	}

	@Test
	public void testSingle() {
		Single<Hello> single = this.testClient.getHelloSingle();
		assertNotNull("single was null", single);
		Hello hello = single.toBlocking().value();
		assertNotNull("hello was null", hello);
		assertEquals("first hello didn't match", new Hello(HELLO_WORLD_1), hello);
	}

	@Test
	public void testNoContentResponse() {
		ResponseEntity<Void> response = testClient.noContent();
		assertNotNull("response was null", response);
		assertEquals("status code was wrong", HttpStatus.NO_CONTENT,
				response.getStatusCode());
	}

	@Test
	public void testHeadResponse() {
		ResponseEntity<Void> response = testClient.head();
		assertNotNull("response was null", response);
		assertEquals("status code was wrong", HttpStatus.OK, response.getStatusCode());
	}

	@Test
	public void testHttpEntity() {
		HttpEntity<Hello> entity = testClient.getHelloEntity();
		assertNotNull("entity was null", entity);
		Hello hello = entity.getBody();
		assertNotNull("hello was null", hello);
		assertEquals("first hello didn't match", new Hello(HELLO_WORLD_1), hello);
	}

	@Test
	public void testMoreComplexHeader() {
		String response = testClient.moreComplexContentType("{\"value\":\"OK\"}");
		assertNotNull("response was null", response);
		assertEquals("didn't respond with {\"value\":\"OK\"}", "{\"value\":\"OK\"}",
				response);
	}

	@Test
	public void testDecodeNotFound() {
		ResponseEntity<String> response = decodingTestClient.notFound();
		assertNotNull("response was null", response);
		assertEquals("status code was wrong", HttpStatus.NOT_FOUND,
				response.getStatusCode());
		assertNull("response body was not null", response.getBody());
	}

	@Test
	public void testConvertingExpander() {
		assertEquals(Arg.A.toString(), testClient.getToString(Arg.A));
		assertEquals(Arg.B.toString(), testClient.getToString(Arg.B));

		assertEquals("bar", testClient.getToString(new OtherArg("foo")));
		List<OtherArg> args = new ArrayList<>();
		args.add(new OtherArg("foo"));
		args.add(new OtherArg("goo"));
		List<String> expectedResult = new ArrayList<>();
		expectedResult.add("bar");
		expectedResult.add("goo");
		assertEquals(expectedResult, testClient.getToString(args));
	}

	@Test
	public void testHystrixFallbackWorks() {
		Hello hello = hystrixClient.fail();
		assertNotNull("hello was null", hello);
		assertEquals("message was wrong", "fallback", hello.getMessage());
	}

	@Test
	public void testHystrixFallbackSingle() {
		Single<Hello> single = hystrixClient.failSingle();
		assertNotNull("single was null", single);
		Hello hello = single.toBlocking().value();
		assertNotNull("hello was null", hello);
		assertEquals("message was wrong", "fallbacksingle", hello.getMessage());
	}

	@Test
	public void testHystrixFallbackCommand() {
		HystrixCommand<Hello> command = hystrixClient.failCommand();
		assertNotNull("command was null", command);
		Hello hello = command.execute();
		assertNotNull("hello was null", hello);
		assertEquals("message was wrong", "fallbackcommand", hello.getMessage());
	}

	@Test
	public void testHystrixFallbackObservable() {
		Observable<Hello> observable = hystrixClient.failObservable();
		assertNotNull("observable was null", observable);
		Hello hello = observable.toBlocking().first();
		assertNotNull("hello was null", hello);
		assertEquals("message was wrong", "fallbackobservable", hello.getMessage());
	}

	@Test
	public void testHystrixFallbackFuture() throws Exception {
		Future<Hello> future = hystrixClient.failFuture();
		assertNotNull("future was null", future);
		Hello hello = future.get(1, TimeUnit.SECONDS);
		assertNotNull("hello was null", hello);
		assertEquals("message was wrong", "fallbackfuture", hello.getMessage());
	}

	@Test
	public void testHystrixClientWithFallBackFactory() throws Exception {
		Hello hello = hystrixClientWithFallBackFactory.fail();
		assertNotNull("hello was null", hello);
		assertNotNull("hello#message was null", hello.getMessage());
		assertTrue("hello#message did not contain the cause (status code) of the fallback invocation",
			hello.getMessage().contains("500"));
	}

	@Test
	public void namedFeignClientWorks() {
		assertNotNull("namedHystrixClient was null", this.namedHystrixClient);
	}

	@Test
	public void testHystrixSetterFactory() {
		HystrixCommand<List<Hello>> command = this.hystrixSetterFactoryClient
			.getHellosHystrix();
		assertNotNull("command was null", command);
		String setterPrefix = TestHystrixSetterFactoryClientConfig.SETTER_PREFIX;
		assertEquals(
			"Hystrix command group name should match the name of the feign client with a prefix of "
				+ setterPrefix, setterPrefix + "localapp5",
			command.getCommandGroup().name());
		assertEquals(
			"Hystrix command key name should match the request method (space) request path with a prefix of "
				+ setterPrefix, setterPrefix + "GET /hellos",
			command.getCommandKey().name());
		List<Hello> hellos = command.execute();
		assertNotNull("hellos was null", hellos);
		assertEquals("hellos didn't match", hellos, getHelloList());
	}

	public static class Hello {
		private String message;

		public Hello() {
		}

		public Hello(String message) {
			this.message = message;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Hello that = (Hello) o;
			return Objects.equals(message, that.message);
		}

		@Override
		public int hashCode() {
			return Objects.hash(message);
		}
	}

	@Configuration
	public static class TestDefaultFeignConfig {
		@Bean
		Logger.Level feignLoggerLevel() {
			return Logger.Level.FULL;
		}
	}

	// Load balancer with fixed server list for "local" pointing to localhost
	@Configuration
	public static class LocalRibbonClientConfiguration {

		@Value("${local.server.port}")
		private int port = 0;

		@Bean
		public ServerList<Server> ribbonServerList() {
			return new StaticServerList<>(new Server("localhost", this.port));
		}

	}
}
