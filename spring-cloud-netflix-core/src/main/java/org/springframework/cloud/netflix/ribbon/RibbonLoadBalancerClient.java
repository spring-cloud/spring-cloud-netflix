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

import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;

import com.google.common.base.Throwables;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerStats;
import com.netflix.servo.monitor.Stopwatch;

/**
 * @author Spencer Gibb
 * @author Dave Syer
 */
public class RibbonLoadBalancerClient implements LoadBalancerClient {

	private SpringClientFactory clientFactory;

	public RibbonLoadBalancerClient(SpringClientFactory clientFactory) {
		this.clientFactory = clientFactory;
	}

	@Override
	public URI reconstructURI(ServiceInstance instance, URI original) {
		String serviceId = instance.getServiceId();
		RibbonLoadBalancerContext context = this.clientFactory
				.getLoadBalancerContext(serviceId);
		Server server = new Server(instance.getHost(), instance.getPort());
		return context.reconstructURIWithServer(server, original);
	}

	@Override
	public ServiceInstance choose(String serviceId) {
		return new RibbonServer(serviceId, getServer(serviceId));
	}

	@Override
	public <T> T execute(String serviceId, LoadBalancerRequest<T> request) {
		ILoadBalancer loadBalancer = getLoadBalancer(serviceId);
		RibbonLoadBalancerContext context = this.clientFactory
				.getLoadBalancerContext(serviceId);
		Server server = getServer(serviceId, loadBalancer);
		RibbonServer ribbonServer = new RibbonServer(serviceId, server);

		ServerStats serverStats = context.getServerStats(server);
		context.noteOpenConnection(serverStats);
		Stopwatch tracer = context.getExecuteTracer().start();

		try {
			T returnVal = request.apply(ribbonServer);
			recordStats(context, tracer, serverStats, returnVal, null);
			return returnVal;
		}
		catch (Exception ex) {
			recordStats(context, tracer, serverStats, null, ex);
			Throwables.propagate(ex);
		}
		return null;
	}

	private void recordStats(RibbonLoadBalancerContext context, Stopwatch tracer,
			ServerStats serverStats, Object entity, Throwable exception) {
		tracer.stop();
		long duration = tracer.getDuration(TimeUnit.MILLISECONDS);
		context.noteRequestCompletion(serverStats, entity, exception, duration, null/* errorHandler */);
	}

	protected Server getServer(String serviceId) {
		return getServer(serviceId, getLoadBalancer(serviceId));
	}

	protected Server getServer(String serviceId, ILoadBalancer loadBalancer) {
		Server server = loadBalancer.chooseServer("default");
		if (server == null) {
			throw new IllegalStateException(
					"Unable to locate ILoadBalancer for service: " + serviceId);
		}
		return server;
	}

	protected ILoadBalancer getLoadBalancer(String serviceId) {
		return this.clientFactory.getLoadBalancer(serviceId);
	}

	protected static class RibbonServer implements ServiceInstance {
		protected String serviceId;
		protected Server server;

		protected RibbonServer(String serviceId, Server server) {
			this.serviceId = serviceId;
			this.server = server;
		}

		@Override
		public String getServiceId() {
			return this.serviceId;
		}

		@Override
		public String getHost() {
			return this.server.getHost();
		}

		@Override
		public int getPort() {
			return this.server.getPort();
		}

	}

}
