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

package org.springframework.cloud.netflix.turbine.stream;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.test.rule.OutputCapture;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import rx.subjects.PublishSubject;

public class HystrixStreamAggregatorTests {

	private ObjectMapper mapper = new ObjectMapper();

	private PublishSubject<Map<String, Object>> publisher = PublishSubject.create();

	private HystrixStreamAggregator aggregator = new HystrixStreamAggregator(this.mapper,
			this.publisher);

	@Rule
	public OutputCapture output = new OutputCapture();

	@Test
	public void messageDecoded() throws Exception {
		this.publisher.subscribe(map -> {
			assertThat(map.get("type"), equalTo("HystrixCommand"));
		});
		this.aggregator.sendToSubject(PAYLOAD);
		this.output.expect(not(containsString("ERROR")));
	}

	@Test
	public void messageWrappedInArray() throws Exception {
		this.publisher.subscribe(map -> {
			assertThat(map.get("type"), equalTo("HystrixCommand"));
		});
		this.aggregator.sendToSubject("[" + PAYLOAD + "]");
		this.output.expect(not(containsString("ERROR")));
	}

	@Test
	public void doubleEncodedMessage() throws Exception {
		this.publisher.subscribe(map -> {
			assertThat(map.get("type"), equalTo("HystrixCommand"));
		});
		// If The JSON is embedded in a JSON String this is what it looks like
		String payload = "\"" + PAYLOAD.replace("\"", "\\\"") + "\"";
		this.aggregator.sendToSubject(payload);
		this.output.expect(not(containsString("ERROR")));
	}

	private static String PAYLOAD = "{\"origin\":{\"host\":\"dsyer\",\"port\":-1,\"serviceId\":\"application\",\"id\":\"application\"},\"data\":{\"type\":\"HystrixCommand\",\"name\":\"application.ok\",\"group\":\"MyService\",\"currentTime\":1457089387160,\"isCircuitBreakerOpen\":false,\"errorPercentage\":0,\"errorCount\":0,\"requestCount\":0,\"rollingCountCollapsedRequests\":0,\"rollingCountExceptionsThrown\":0,\"rollingCountFailure\":0,\"rollingCountFallbackFailure\":0,\"rollingCountFallbackRejection\":0,\"rollingCountFallbackSuccess\":0,\"rollingCountResponsesFromCache\":0,\"rollingCountSemaphoreRejected\":0,\"rollingCountShortCircuited\":0,\"rollingCountSuccess\":1,\"rollingCountThreadPoolRejected\":0,\"rollingCountTimeout\":0,\"currentConcurrentExecutionCount\":0,\"latencyExecute_mean\":0,\"latencyExecute\":{\"0\":0,\"25\":0,\"50\":0,\"75\":0,\"90\":0,\"95\":0,\"99\":0,\"99.5\":0,\"100\":0},\"latencyTotal_mean\":0,\"latencyTotal\":{\"0\":0,\"25\":0,\"50\":0,\"75\":0,\"90\":0,\"95\":0,\"99\":0,\"99.5\":0,\"100\":0},\"propertyValue_circuitBreakerRequestVolumeThreshold\":20,\"propertyValue_circuitBreakerSleepWindowInMilliseconds\":5000,\"propertyValue_circuitBreakerErrorThresholdPercentage\":50,\"propertyValue_circuitBreakerForceOpen\":false,\"propertyValue_circuitBreakerForceClosed\":false,\"propertyValue_circuitBreakerEnabled\":true,\"propertyValue_executionIsolationStrategy\":\"THREAD\",\"propertyValue_executionIsolationThreadTimeoutInMilliseconds\":1000,\"propertyValue_executionIsolationThreadInterruptOnTimeout\":true,\"propertyValue_executionIsolationThreadPoolKeyOverride\":null,\"propertyValue_executionIsolationSemaphoreMaxConcurrentRequests\":10,\"propertyValue_fallbackIsolationSemaphoreMaxConcurrentRequests\":10,\"propertyValue_metricsRollingStatisticalWindowInMilliseconds\":10000,\"propertyValue_requestCacheEnabled\":true,\"propertyValue_requestLogEnabled\":true,\"reportingHosts\":1}}";
}
