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

package org.springframework.cloud.netflix.turbine.amqp;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.text.sse.ServerSentEvent;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.SocketUtils;

import rx.Observable;
import rx.subjects.PublishSubject;

import com.netflix.turbine.aggregator.InstanceKey;
import com.netflix.turbine.aggregator.StreamAggregator;
import com.netflix.turbine.internal.JsonUtility;

import static io.reactivex.netty.pipeline.PipelineConfigurators.sseServerConfigurator;

/**
 * @author Spencer Gibb
 */
@Configuration
@Slf4j
@EnableConfigurationProperties(TurbineAmqpProperties.class)
public class TurbineAmqpConfiguration implements SmartLifecycle {

	private boolean running = false;

	@Autowired
	private TurbineAmqpProperties turbine;
	private int turbinePort;

	@Bean
	public PublishSubject<Map<String, Object>> hystrixSubject() {
		return PublishSubject.create();
	}

	@Bean
	public HttpServer<ByteBuf, ServerSentEvent> aggregatorServer() {
		// multicast so multiple concurrent subscribers get the same stream
		Observable<Map<String, Object>> publishedStreams = StreamAggregator
				.aggregateGroupedStreams(
						hystrixSubject().groupBy(
								data -> InstanceKey.create((String) data
										.get("instanceId"))))
				.doOnUnsubscribe(() -> log.info("Unsubscribing aggregation."))
				.doOnSubscribe(() -> log.info("Starting aggregation")).flatMap(o -> o)
				.publish().refCount();

		this.turbinePort = this.turbine.getPort();

		if (this.turbinePort <= 0) {
			this.turbinePort = SocketUtils.findAvailableTcpPort(40000);
		}

		HttpServer<ByteBuf, ServerSentEvent> httpServer = RxNetty.createHttpServer(
				this.turbinePort,
				(request, response) -> {
					log.info("SSE Request Received");
					response.getHeaders().setHeader("Content-Type", "text/event-stream");
					return publishedStreams.doOnUnsubscribe(
							() -> log.info("Unsubscribing RxNetty server connection"))
							.flatMap(
									data -> response.writeAndFlush(new ServerSentEvent(
											null, null, JsonUtility.mapToJson(data))));
				}, sseServerConfigurator());
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
		aggregatorServer().start();
	}

	@Override
	public void stop() {
		try {
			aggregatorServer().shutdown();
		}
		catch (InterruptedException e) {
			log.error("Error shutting down", e);
		}
		this.running = false;
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public int getPhase() {
		return 0;
	}

	public int getTurbinePort() {
		return this.turbinePort;
	}
}
