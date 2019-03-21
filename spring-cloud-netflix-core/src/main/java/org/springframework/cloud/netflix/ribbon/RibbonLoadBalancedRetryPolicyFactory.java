/*
 * Copyright 2013-2016 the original author or authors.
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

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryContext;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicy;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicyFactory;
import org.springframework.cloud.client.loadbalancer.ServiceInstanceChooser;
import org.springframework.http.HttpMethod;

/**
 * @author Ryan Baxter
 */
public class RibbonLoadBalancedRetryPolicyFactory implements LoadBalancedRetryPolicyFactory {

    private SpringClientFactory clientFactory;

    public RibbonLoadBalancedRetryPolicyFactory(SpringClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    @Override
    public LoadBalancedRetryPolicy create(final String serviceId, final ServiceInstanceChooser loadBalanceChooser) {
        final RibbonLoadBalancerContext lbContext = this.clientFactory
                .getLoadBalancerContext(serviceId);
        return new LoadBalancedRetryPolicy() {
            private int sameServerCount = 0;
            private int nextServerCount = 0;
            private ServiceInstance lastServiceInstance = null;
            public boolean canRetry(LoadBalancedRetryContext context) {
                HttpMethod method = context.getRequest().getMethod();
                return HttpMethod.GET == method || lbContext.isOkToRetryOnAllOperations();
            }

            @Override
            public boolean canRetrySameServer(LoadBalancedRetryContext context) {
                return sameServerCount < lbContext.getRetryHandler().getMaxRetriesOnSameServer() && canRetry(context);
            }

            @Override
            public boolean canRetryNextServer(LoadBalancedRetryContext context) {
                //this will be called after a failure occurs and we increment the counter
                //so we check that the count is less than or equals to too make sure
                //we try the next server the right number of times
                return nextServerCount <= lbContext.getRetryHandler().getMaxRetriesOnNextServer() && canRetry(context);
            }

            @Override
            public void close(LoadBalancedRetryContext context) {

            }

            @Override
            public void registerThrowable(LoadBalancedRetryContext context, Throwable throwable) {
                //Check if we need to ask the load balancer for a new server.
                //Do this before we increment the counters because the first call to this method
                //is not a retry it is just an initial failure.
                if(!canRetrySameServer(context)  && canRetryNextServer(context)) {
                    context.setServiceInstance(loadBalanceChooser.choose(serviceId));
                }
                //This method is called regardless of whether we are retrying or making the first request.
                //Since we do not count the initial request in the retry count we don't reset the counter
                //until we actually equal the same server count limit.  This will allow us to make the initial
                //request plus the right number of retries.
                if(sameServerCount >= lbContext.getRetryHandler().getMaxRetriesOnSameServer() && canRetry(context)) {
                    //reset same server since we are moving to a new server
                    sameServerCount = 0;
                    nextServerCount++;
                    if(!canRetryNextServer(context)) {
                        context.setExhaustedOnly();
                    }
                } else {
                    sameServerCount++;
                }

            }
        };
    }
}
