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

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.util.UriComponentsBuilder;

import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.IClientConfig;
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
		Assert.notNull(instance, "instance can not be null");
		String serviceId = instance.getServiceId();
		RibbonLoadBalancerContext context = this.clientFactory
				.getLoadBalancerContext(serviceId);
		Server server = new Server(instance.getHost(), instance.getPort());
		boolean secure = isSecure(this.clientFactory, serviceId);
		URI uri = original;
		if(secure) {
			uri = UriComponentsBuilder.fromUri(uri).scheme("https").build().toUri();
		}
		return context.reconstructURIWithServer(server, uri);
	}

	@Override
	public ServiceInstance choose(String serviceId) {
		Server server = getServer(serviceId);
		if (server == null) {
			return null;
		}
		return new RibbonServer(serviceId, server, isSecure(this.clientFactory, serviceId));
	}

	@Override
	public <T> T execute(String serviceId, LoadBalancerRequest<T> request) {
		ILoadBalancer loadBalancer = getLoadBalancer(serviceId);
		RibbonLoadBalancerContext context = this.clientFactory
				.getLoadBalancerContext(serviceId);
		Server server = getServer(loadBalancer);
		RibbonServer ribbonServer = new RibbonServer(serviceId, server, isSecure(clientFactory, serviceId));

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
			ReflectionUtils.rethrowRuntimeException(ex);
		}
		return null;
	}
	
	private boolean isSecure(SpringClientFactory clientFactory, String serviceId) {
		IClientConfig config = clientFactory.getClientConfig(serviceId);
		if(config != null) {
			return config.get(CommonClientConfigKey.IsSecure, false);
		}
		return false;
	}

	private void recordStats(RibbonLoadBalancerContext context, Stopwatch tracer,
			ServerStats serverStats, Object entity, Throwable exception) {
		tracer.stop();
		long duration = tracer.getDuration(TimeUnit.MILLISECONDS);
		context.noteRequestCompletion(serverStats, entity, exception, duration, null/* errorHandler */);
	}

	protected Server getServer(String serviceId) {
		return getServer(getLoadBalancer(serviceId));
	}

	protected Server getServer(ILoadBalancer loadBalancer) {
		if (loadBalancer == null) {
			return null;
		}
		return loadBalancer.chooseServer("default"); //TODO: better handling of key
	}

	protected ILoadBalancer getLoadBalancer(String serviceId) {
		return this.clientFactory.getLoadBalancer(serviceId);
	}

	protected static class RibbonServer implements ServiceInstance {
		private String serviceId;
		private Server server;
		private boolean secure;
		
		protected RibbonServer(String serviceId, Server server) {
			this(serviceId, server, false);
		}

		protected RibbonServer(String serviceId, Server server, boolean secure) {
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

		@Override
		public boolean isSecure() {
			return this.secure;
		}

		@Override
		public URI getUri() {
			return DefaultServiceInstance.getUri(this);
		}

		public Server getServer() {
			return this.server;
		}
	}

}
