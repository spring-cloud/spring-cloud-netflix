package org.springframework.cloud.netflix.feign.support;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixThreadPoolKey;

/**
 * Convenience class for implementing feign fallbacks that return {@link HystrixCommand}.
 * Also useful for return types of {@link rx.Observable} and {@link java.util.concurrent.Future}.
 * For those return types, just call {@link FallbackCommand#observe()} or {@link FallbackCommand#queue()} respectively.
 * @author Spencer Gibb
 */
public class FallbackCommand<T> extends HystrixCommand<T> {

	private T result;

	public FallbackCommand(T result) {
		this(result, "fallback");
	}

	protected FallbackCommand(T result, String groupname) {
		super(HystrixCommandGroupKey.Factory.asKey(groupname));
		this.result = result;
	}

	public FallbackCommand(T result, HystrixCommandGroupKey group) {
		super(group);
		this.result = result;
	}

	public FallbackCommand(T result, HystrixCommandGroupKey group, int executionIsolationThreadTimeoutInMilliseconds) {
		super(group, executionIsolationThreadTimeoutInMilliseconds);
		this.result = result;
	}

	public FallbackCommand(T result, HystrixCommandGroupKey group, HystrixThreadPoolKey threadPool) {
		super(group, threadPool);
		this.result = result;
	}

	public FallbackCommand(T result, HystrixCommandGroupKey group, HystrixThreadPoolKey threadPool, int executionIsolationThreadTimeoutInMilliseconds) {
		super(group, threadPool, executionIsolationThreadTimeoutInMilliseconds);
		this.result = result;
	}

	public FallbackCommand(T result, Setter setter) {
		super(setter);
		this.result = result;
	}

	@Override
	protected T run() throws Exception {
		return this.result;
	}
}
