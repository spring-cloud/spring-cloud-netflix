package org.springframework.cloud.netflix.feign.hystrix;

import com.netflix.hystrix.HystrixCommand;

public interface CustomCommandHook {
	<T> void onRunStart(HystrixCommand<T> commandInstance);
}
