package org.springframework.cloud.netflix.ribbon.support;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.InterceptorRetryPolicy;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryContext;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicy;
import org.springframework.cloud.client.loadbalancer.ServiceInstanceChooser;
import org.springframework.http.HttpRequest;
import org.springframework.retry.RetryContext;

/**
 * @author Ryan Baxter
 */
public class RibbonRetryPolicy extends InterceptorRetryPolicy {
	private HttpRequest request;
	private String serviceId;
	public RibbonRetryPolicy(HttpRequest request, LoadBalancedRetryPolicy policy, ServiceInstanceChooser serviceInstanceChooser, String serviceName) {
		super(request, policy, serviceInstanceChooser, serviceName);
		this.request = request;
		this.serviceId = serviceName;
	}

	@Override
	public boolean canRetry(RetryContext context) {
		/*
		 * In InterceptorRetryPolicy.canRetry we ask the LoadBalancer to choose a server if one is not
		 * set in the retry context and then return true.  RetryTemplat calls the canRetry method of
		 * the policy even on its first execution.  So the fact that we didnt have a service instance set
		 * in the RetryContext signaled that it was the first execution and we should return true.
		 *
		 */
		if(context.getRetryCount() == 0) {
			return true;
		}
		return super.canRetry(context);
	}

	@Override
	public RetryContext open(RetryContext parent) {
		LoadBalancedRetryContext context = new LoadBalancedRetryContext(parent, this.request);
		context.setServiceInstance(new RibbonRetryPolicyServiceInstance(serviceId, request));
		return context;
	}

	class RibbonRetryPolicyServiceInstance implements ServiceInstance {

		private String serviceId;
		private HttpRequest request;
		private Map<String, String> metadata;

		RibbonRetryPolicyServiceInstance(String serviceId, HttpRequest request) {
			this.serviceId = serviceId;
			this.request = request;
			this.metadata = new HashMap<>();
		}

		@Override
		public String getServiceId() {
			return serviceId;
		}

		@Override
		public String getHost() {
			return request.getURI().getHost();
		}

		@Override
		public int getPort() {
			return request.getURI().getPort();
		}

		@Override
		public boolean isSecure() {
			return "https".equals(request.getURI().getScheme());
		}

		@Override
		public URI getUri() {
			return request.getURI();
		}

		@Override
		public Map<String, String> getMetadata() {
			return metadata;
		}
	}
}
