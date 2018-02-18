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

import static io.reactivex.netty.pipeline.PipelineConfigurators.serveSseConfigurator;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.SocketUtils;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;
import rx.subjects.PublishSubject;

/**
 * @author Spencer Gibb
 * @author Daniel Lavoie
 */
@Configuration
@EnableConfigurationProperties(TurbineStreamProperties.class)
public class TurbineStreamConfiguration {
	private static final Log log = LogFactory.getLog(TurbineStreamConfiguration.class);

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
	@ConditionalOnMissingBean
	public TurbineSecurityFilter turbineSecurityFilter() {
		return new DefaultTurbineSecurityFilter();
	}

	@Bean
	public RequestHandler<ByteBuf, ServerSentEvent> aggregatorRequestHandler(
			PublishSubject<Map<String, Object>> hystrixSubject,
			TurbineSecurityFilter turbineSecurityFilter) {
		return new AggregatorRequestHandler(hystrixSubject, turbineSecurityFilter);
	}
	
	@Bean
	public TurbineLifecycle turbineLifecycle(
			HttpServer<ByteBuf, ServerSentEvent> aggregatorServer) {
		return new TurbineLifecycle(aggregatorServer);
	}

	@Bean
	public HttpServer<ByteBuf, ServerSentEvent> aggregatorServer(
			TurbineStreamProperties properties,
			RequestHandler<ByteBuf, ServerSentEvent> aggregatorRequestHandler) {
		this.turbinePort = properties.getPort();

		if (this.turbinePort <= 0) {
			this.turbinePort = SocketUtils.findAvailableTcpPort(40000);
		}

		log.info("Starting turbine on port " + this.turbinePort);

		HttpServer<ByteBuf, ServerSentEvent> httpServer = RxNetty.createHttpServer(
				this.turbinePort, aggregatorRequestHandler, serveSseConfigurator());
		
		return httpServer;
	}

	public int getTurbinePort() {
		return this.turbinePort;
	}

}
