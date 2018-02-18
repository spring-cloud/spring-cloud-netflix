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

package org.springframework.cloud.netflix.turbine.stream;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.netflix.turbine.aggregator.InstanceKey;
import com.netflix.turbine.aggregator.StreamAggregator;
import com.netflix.turbine.internal.JsonUtility;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;
import rx.Observable;
import rx.subjects.PublishSubject;

/**
 * @author Daniel Lavoie
 */
public class AggregatorRequestHandler
		implements RequestHandler<ByteBuf, ServerSentEvent> {
	private static final Log log = LogFactory.getLog(AggregatorRequestHandler.class);

	private PublishSubject<Map<String, Object>> hystrixSubject;
	private TurbineSecurityFilter turbineSecurityFilter;

	private Observable<Map<String, Object>> turbineStream;

	public AggregatorRequestHandler(PublishSubject<Map<String, Object>> hystrixSubject,
			TurbineSecurityFilter turbineSecurityFilter) {
		this.hystrixSubject = hystrixSubject;
		this.turbineSecurityFilter = turbineSecurityFilter;

		this.turbineStream = initTurbineStream();

	}

	@Override
	public Observable<Void> handle(HttpServerRequest<ByteBuf> request,
			HttpServerResponse<ServerSentEvent> response) {
		log.info("SSE Request Received");

		return turbineSecurityFilter.validateRequest(request, response)

				.flatMap(
						isRequestValid -> routeRequest(isRequestValid, request, response))

				.doOnUnsubscribe(
						() -> log.info("Unsubscribing RxNetty server connection"));
	}

	private Observable<Void> routeRequest(boolean isRequestValid,
			HttpServerRequest<ByteBuf> request,
			HttpServerResponse<ServerSentEvent> response) {
		if (isRequestValid) {
			return turbineSecurityFilter.hasPermission(request, response)

					.flatMap(hasPermission -> sendTurbineEvent(hasPermission, response));
		}
		else {
			return response.close();
		}
	}

	private Observable<Map<String, Object>> initTurbineStream() {
		// multicast so multiple concurrent subscribers get the same stream
		Observable<Map<String, Object>> publishedStreams = StreamAggregator
				.aggregateGroupedStreams(hystrixSubject.groupBy(
						data -> InstanceKey.create((String) data.get("instanceId"))))

				.doOnUnsubscribe(() -> log.info("Unsubscribing aggregation"))
				.doOnSubscribe(() -> log.info("Starting aggregation")).flatMap(o -> o)
				.publish().refCount();

		Observable<Map<String, Object>> ping = Observable
				.interval(1, 10, TimeUnit.SECONDS)
				.map(count -> Collections.singletonMap("type", (Object) "Ping")).publish()
				.refCount();

		return Observable.merge(publishedStreams, ping);
	}

	private Observable<Void> sendTurbineEvent(boolean hasPermission,
			HttpServerResponse<ServerSentEvent> response) {
		if (!hasPermission) {
			return response.close();
		}
		else {
			return turbineStream

					.doOnEach(data -> response.getHeaders().setHeader("Content-Type",
							"text/event-stream"))

					.map(data -> new ServerSentEvent(null,
							Unpooled.copiedBuffer("message", StandardCharsets.UTF_8),
							Unpooled.copiedBuffer(JsonUtility.mapToJson(data) + "\n",
									StandardCharsets.UTF_8)))

					.flatMap(serverSentEvent -> response.writeAndFlush(serverSentEvent))

					.doOnError(ex -> log.error("Error while writing stream data.", ex))

					.onErrorResumeNext(ex -> response.close());
		}
	}
}
