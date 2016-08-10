package org.springframework.cloud.netflix.hystrix.security.app;

import java.util.concurrent.Callable;

import org.springframework.stereotype.Component;

import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;

@Component
public class CustomConcurrenyStrategy extends HystrixConcurrencyStrategy {
	private boolean hookCalled;

	@Override
	public <T> Callable<T> wrapCallable(Callable<T> callable) {
		this.hookCalled = true;

		return super.wrapCallable(callable);
	}

	public boolean isHookCalled() {
		return hookCalled;
	}
}
