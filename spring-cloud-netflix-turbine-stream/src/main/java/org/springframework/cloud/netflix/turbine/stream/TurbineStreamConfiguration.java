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
import java.util.concurrent.atomic.AtomicBoolean;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;

import com.netflix.turbine.aggregator.InstanceKey;
import com.netflix.turbine.aggregator.StreamAggregator;
import com.netflix.turbine.internal.JsonUtility;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.SocketUtils;

import static io.reactivex.netty.pipeline.PipelineConfigurators.serveSseConfigurator;

import rx.Observable;
import rx.subjects.PublishSubject;

/**
 * @author Spencer Gibb
 * @author Daniel Lavoie
 */
@Configuration
@EnableConfigurationProperties(TurbineStreamProperties.class)
public class TurbineStreamConfiguration implements SmartLifecycle {

	private static final Log log = LogFactory.getLog(TurbineStreamConfiguration.class);

	private AtomicBoolean running = new AtomicBoolean(false);

	@Autowired
	private TurbineStreamProperties properties;

	private int turbinePort;

	@Bean
	public HasFeatures Feature() {
		return HasFeatures.namedFeature("Turbine (Stream)",
				TurbineStreamProperties.class);
	}

	@Bean
	public PublishSubject<Map<String, Object>> hystrixSubject() {
		return PublishSubject.create();
	}

	@Bean
	@SuppressWarnings("deprecation")
	public HttpServer<ByteBuf, ServerSentEvent> aggregatorServer() {
		// multicast so multiple concurrent subscribers get the same stream
		Observable<Map<String, Object>> publishedStreams = StreamAggregator
				.aggregateGroupedStreams(hystrixSubject().groupBy(
						data -> InstanceKey.create((String) data.get("instanceId"))))
				.doOnUnsubscribe(() -> log.info("Unsubscribing aggregation."))
				.doOnSubscribe(() -> log.info("Starting aggregation")).flatMap(o -> o)
				.publish().refCount();
		Observable<Map<String, Object>> ping = Observable.timer(1, 10, TimeUnit.SECONDS)
				.map(count -> Collections.singletonMap("type", (Object) "Ping")).publish()
				.refCount();
		Observable<Map<String, Object>> output = Observable.merge(publishedStreams, ping);

		this.turbinePort = this.properties.getPort();

		if (this.turbinePort <= 0) {
			this.turbinePort = SocketUtils.findAvailableTcpPort(40000);
		}

		HttpServer<ByteBuf, ServerSentEvent> httpServer = RxNetty
				.createHttpServer(this.turbinePort, (request, response) -> {
					log.info("SSE Request Received");
					response.getHeaders().setHeader("Content-Type", "text/event-stream");
					return output.doOnUnsubscribe(
							() -> log.info("Unsubscribing RxNetty server connection"))
							.flatMap(data -> response.writeAndFlush(new ServerSentEvent(
									null,
									Unpooled.copiedBuffer("message",
											StandardCharsets.UTF_8),
									Unpooled.copiedBuffer(JsonUtility.mapToJson(data)+"\n",
											StandardCharsets.UTF_8))));
				}, serveSseConfigurator());
		return httpServer;
	}

	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public void stop(Runnable callback) {
		stop();
		callback.run();
	}

	@Override
	public void start() {
		if (this.running.compareAndSet(false, true)) {
			aggregatorServer().start();
		}
	}

	@Override
	public void stop() {
		if (this.running.compareAndSet(true, false)) {
			try {
				aggregatorServer().shutdown();
			}
			catch (InterruptedException ex) {
				log.error("Error shutting down", ex);
			}
		}
	}

	@Override
	public boolean isRunning() {
		return this.running.get();
	}

	@Override
	public int getPhase() {
		return 0;
	}

	public int getTurbinePort() {
		return this.turbinePort;
	}

}
