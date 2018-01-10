package org.springframework.cloud.netflix.ribbon;

import org.springframework.cloud.client.loadbalancer.RetryableStatusCodeException;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryException;

import java.net.URI;

public abstract class RibbonRecoveryCallback<T, R> implements RecoveryCallback<T> {

	protected abstract T createResponse(R response, URI uri);

	@Override
	public T recover(RetryContext context) throws Exception {
		Throwable lastThrowable = context.getLastThrowable();
		if (lastThrowable != null && lastThrowable instanceof RetryableStatusCodeException) {
			RetryableStatusCodeException ex = (RetryableStatusCodeException) lastThrowable;
			return createResponse((R) ex.getResponse(), ex.getUri());
		}
		throw new RetryException("Could not recover", lastThrowable);
	}
}
