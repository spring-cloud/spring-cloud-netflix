package org.springframework.netflix.turbine.amqp;

import com.netflix.turbine.aggregator.InstanceKey;
import com.netflix.turbine.aggregator.StreamAggregator;
import com.netflix.turbine.internal.JsonUtility;
import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.text.sse.ServerSentEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import rx.Observable;
import rx.subjects.PublishSubject;

import java.util.Map;

import static io.reactivex.netty.pipeline.PipelineConfigurators.sseServerConfigurator;

/**
 * @author Spencer Gibb
 */
@Configuration
@Slf4j
@ConfigurationProperties("bus.turbine")
public class TurbineAmqpConfiguration implements SmartLifecycle {

	private boolean running = false;

	private int port = 8989;

	@Bean
	public PublishSubject<Map<String, Object>> hystrixSubject() {
		return PublishSubject.create();
	}

	@Bean
	public HttpServer<ByteBuf, ServerSentEvent> aggregatorServer() {
		// multicast so multiple concurrent subscribers get the same stream
		Observable<Map<String, Object>> publishedStreams = StreamAggregator.aggregateGroupedStreams(hystrixSubject()
				.groupBy(data -> InstanceKey.create((String) data.get("instanceId"))))
				.doOnUnsubscribe(() -> log.info("BusTurbine => Unsubscribing aggregation."))
				.doOnSubscribe(() -> log.info("BusTurbine => Starting aggregation"))
				.flatMap(o -> o).publish().refCount();

		HttpServer<ByteBuf, ServerSentEvent> httpServer = RxNetty.createHttpServer(port, (request, response) -> {
			log.info("BusTurbine => SSE Request Received");
			response.getHeaders().setHeader("Content-Type", "text/event-stream");
			return publishedStreams
					.doOnUnsubscribe(() -> log.info("BusTurbine => Unsubscribing RxNetty server connection"))
					.flatMap(data -> response.writeAndFlush(new ServerSentEvent(null, null, JsonUtility.mapToJson(data))));
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
		} catch (InterruptedException e) {
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
}
