/*
 * Copyright 2016-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.ribbon;

import java.util.concurrent.TimeUnit;

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerStats;
import com.netflix.servo.monitor.Stopwatch;

/**
 * @author Spencer Gibb
 */
public class RibbonStatsRecorder {

	private RibbonLoadBalancerContext context;

	private ServerStats serverStats;

	private Stopwatch tracer;

	public RibbonStatsRecorder(RibbonLoadBalancerContext context, Server server) {
		this.context = context;
		if (server != null) {
			serverStats = context.getServerStats(server);
			context.noteOpenConnection(serverStats);
			tracer = context.getExecuteTracer().start();
		}
	}

	public void recordStats(Object entity) {
		this.recordStats(entity, null);
	}

	public void recordStats(Throwable t) {
		this.recordStats(null, t);
	}

	protected void recordStats(Object entity, Throwable exception) {
		if (this.tracer != null && this.serverStats != null) {
			this.tracer.stop();
			long duration = this.tracer.getDuration(TimeUnit.MILLISECONDS);
			this.context.noteRequestCompletion(serverStats, entity, exception, duration,
					null/* errorHandler */);
		}
	}

}
