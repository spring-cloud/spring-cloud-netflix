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

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.SmartLifecycle;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;

/**
 * @author Daniel Lavoie
 */
public class TurbineLifecycle implements SmartLifecycle {
	private static final Log log = LogFactory.getLog(TurbineStreamConfiguration.class);	
	private HttpServer<ByteBuf, ServerSentEvent> aggregatorServer;
	
	private AtomicBoolean running = new AtomicBoolean(false);
	
	public TurbineLifecycle(HttpServer<ByteBuf, ServerSentEvent> aggregatorServer) {
		this.aggregatorServer = aggregatorServer;
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
			aggregatorServer.start();
		}
	}

	@Override
	public void stop() {
		if (this.running.compareAndSet(true, false)) {
			try {
				aggregatorServer.shutdown();
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
}
