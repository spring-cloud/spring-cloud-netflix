package org.springframework.cloud.netflix.hystrix.security.app;

import java.util.concurrent.Callable;

import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;

import org.springframework.stereotype.Component;

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
