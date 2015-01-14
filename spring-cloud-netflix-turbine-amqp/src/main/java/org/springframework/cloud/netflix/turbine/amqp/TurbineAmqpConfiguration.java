package org.springframework.cloud.netflix.turbine.amqp;

import static io.reactivex.netty.pipeline.PipelineConfigurators.sseServerConfigurator;
import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.text.sse.ServerSentEvent;

import java.util.Map;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.core.env.Environment;
import org.springframework.util.SocketUtils;
import rx.Observable;
import rx.subjects.PublishSubject;

import com.netflix.turbine.aggregator.InstanceKey;
import com.netflix.turbine.aggregator.StreamAggregator;
import com.netflix.turbine.internal.JsonUtility;

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

	@Autowired
	private PortHolder portHolder;

	private Integer turbinePort;

	@Bean
	public PublishSubject<Map<String, Object>> hystrixSubject() {
		return PublishSubject.create();
	}

	@Data
	public static class PortHolder {
		private final Integer port;
	}

	@Configuration
	public static class PortConfig {

		@Autowired
		private TurbineAmqpProperties turbine;

		@Autowired
		private Environment env;

		@Bean
		@ConditionalOnWebApplication
		public PortHolder webPortHolder(ServerProperties serverProperties) {
			return new PortHolder(serverProperties.getPort());
		}

		@Bean
		@ConditionalOnNotWebApplication
		public PortHolder notwebPortHolder() {
			Integer port = new RelaxedPropertyResolver(env).getProperty("server.port", Integer.class, null);
			if (port == null) {
				port = turbine.getPort();
			}
			return new PortHolder(port);
		}
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

		turbinePort = portHolder.getPort();

		if (turbinePort == null || turbinePort <= 0) {
			turbinePort = SocketUtils.findAvailableTcpPort(40000);
		}

		HttpServer<ByteBuf, ServerSentEvent> httpServer = RxNetty.createHttpServer(
				turbinePort,
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
		running = false;
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public int getPhase() {
		return 0;
	}

	public int getTurbinePort() {
		return turbinePort;
	}
}
