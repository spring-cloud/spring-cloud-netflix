/*
 * Copyright 2013-2016 the original author or authors.
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
 *
 */

package org.springframework.cloud.netflix.ribbon.support;

import com.netflix.client.IResponse;
import com.netflix.client.config.IClientConfig;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryContext;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicy;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicyFactory;
import org.springframework.cloud.client.loadbalancer.ServiceInstanceChooser;
import org.springframework.cloud.netflix.feign.ribbon.FeignRetryPolicy;
import org.springframework.http.HttpRequest;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import rx.Observable;
import rx.Subscriber;

import java.net.URISyntaxException;

/**
 * @author Roman Terentiev
 */
public abstract class RetryableClientObservable<RQ extends ContextAwareRequest, RS extends IResponse>
        implements Observable.OnSubscribe<RS> {

    private String clientName;
    private ServiceInstanceChooser serviceInstanceChooser;
    private LoadBalancedRetryPolicyFactory retryPolicyFactory;
    private RQ request;
    private IClientConfig requestConfig;

    public RetryableClientObservable(String clientName, ServiceInstanceChooser serviceInstanceChooser,
                                     LoadBalancedRetryPolicyFactory retryPolicyFactory,
                                     RQ request, IClientConfig requestConfig) {
        this.clientName = clientName;
        this.serviceInstanceChooser = serviceInstanceChooser;
        this.retryPolicyFactory = retryPolicyFactory;
        this.request = request;
        this.requestConfig = requestConfig;
    }

    @Override
    public void call(Subscriber<? super RS> subscriber) {
        try {
            RetryPolicy retryPolicy = getRetryPolicy(request, subscriber);
            RetryCallback<RS, Exception> retryCallback = getRetryCallback(request, requestConfig);

            RetryTemplate retryTemplate = new RetryTemplate();
            retryTemplate.setRetryPolicy(retryPolicy);

            RS response = retryTemplate.execute(retryCallback);

            if (!subscriber.isUnsubscribed()) {
                subscriber.onNext(response);
                subscriber.onCompleted();
            } else {
                response.close();
            }
        } catch (Exception e) {
            if (!subscriber.isUnsubscribed()) {
                subscriber.onError(e);
            }
        }
    }

    protected abstract RQ reconstruct(RQ request, ServiceInstance serviceInstance) throws URISyntaxException;

    protected abstract RS executeInternal(RQ request, IClientConfig requestConfig) throws Exception;

    private RetryCallback<RS, Exception> getRetryCallback(final RQ request, final IClientConfig requestConfig) {
        return new RetryCallback<RS, Exception>() {

            @Override
            public RS doWithRetry(RetryContext context) throws Exception {
                //on retries the policy will choose the server and set it in the context
                //extract the server and update the request being made
                RQ newRequest = request;
                if (context instanceof LoadBalancedRetryContext) {
                    ServiceInstance service = ((LoadBalancedRetryContext) context).getServiceInstance();
                    if (service != null) {
                        //Reconstruct the request URI using the host and port set in the retry context
                        newRequest = reconstruct(request, service);
                    }
                }

                return executeInternal(newRequest, requestConfig);
            }
        };
    }

    private RetryPolicy getRetryPolicy(RQ request, Subscriber<?> subscriber) {
        LoadBalancedRetryPolicy retryPolicy = retryPolicyFactory.create(clientName, serviceInstanceChooser);
        boolean retryable = request.getContext() == null
                || BooleanUtils.toBooleanDefaultIfNull(request.getContext().getRetryable(), true);

        return retryPolicy == null || !retryable
                ? new NeverRetryPolicy()
                : new UnSubscriptionAwareRetryPolicy(subscriber, request, retryPolicy, serviceInstanceChooser, clientName);
    }

    static class UnSubscriptionAwareRetryPolicy extends FeignRetryPolicy {

        private Subscriber<?> subscriber;

        public UnSubscriptionAwareRetryPolicy(Subscriber<?> subscriber, HttpRequest request, LoadBalancedRetryPolicy policy,
                                              ServiceInstanceChooser serviceInstanceChooser, String serviceName) {
            super(request, policy, serviceInstanceChooser, serviceName);
            this.subscriber = subscriber;
        }

        @Override
        public boolean canRetry(RetryContext context) {
            if (subscriber.isUnsubscribed()) {
                context.setExhaustedOnly();
                return false;
            }

            return super.canRetry(context);
        }
    }
}
