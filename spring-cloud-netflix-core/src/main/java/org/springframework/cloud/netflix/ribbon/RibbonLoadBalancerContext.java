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

package org.springframework.cloud.netflix.ribbon;

import com.netflix.client.RetryHandler;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.LoadBalancerContext;
import com.netflix.loadbalancer.ServerStats;
import com.netflix.servo.monitor.Timer;

/**
 * @author Spencer Gibb
 */
public class RibbonLoadBalancerContext extends LoadBalancerContext {
	public RibbonLoadBalancerContext(ILoadBalancer lb) {
		super(lb);
	}

	public RibbonLoadBalancerContext(ILoadBalancer lb, IClientConfig clientConfig) {
		super(lb, clientConfig);
	}

	public RibbonLoadBalancerContext(ILoadBalancer lb, IClientConfig clientConfig,
			RetryHandler handler) {
		super(lb, clientConfig, handler);
	}

	@Override
	public void noteOpenConnection(ServerStats serverStats) {
		super.noteOpenConnection(serverStats);
	}

	@Override
	public Timer getExecuteTracer() {
		return super.getExecuteTracer();
	}

	@Override
	public void noteRequestCompletion(ServerStats stats, Object response, Throwable e,
			long responseTime) {
		super.noteRequestCompletion(stats, response, e, responseTime);
	}

	@Override
	public void noteRequestCompletion(ServerStats stats, Object response, Throwable e,
			long responseTime, RetryHandler errorHandler) {
		super.noteRequestCompletion(stats, response, e, responseTime, errorHandler);
	}

}
