package org.springframework.cloud.netflix.feign;

import org.springframework.beans.BeansException;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

/**
 * @author Eko Kurniawan Khannedy
 */
public class FeignClientHealthIndicator extends AbstractHealthIndicator
		implements ApplicationContextAware {

	private ApplicationContext applicationContext;

	private Class<?> feignClass;

	private String healthMethod;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	public void setFeignClass(String feignClass) throws ClassNotFoundException {
		this.feignClass = Class.forName(feignClass);
	}

	public void setHealthMethod(String healthMethod) {
		this.healthMethod = healthMethod;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		Object feign = applicationContext.getBean(feignClass);
		Method method = ReflectionUtils.findMethod(feignClass, healthMethod);

		try {
			Object result = ReflectionUtils.invokeMethod(method, feign);
			if (result instanceof ResponseEntity) {
				ResponseEntity responseEntity = (ResponseEntity) result;
				if (responseEntity.getStatusCodeValue() == HttpStatus.OK.value()) {
					builder.up();
				} else {
					builder.down();
				}

				builder.withDetail("statusCode", responseEntity.getStatusCode().value())
						.withDetail("responseBody", responseEntity.getBody());
			} else {
				builder.up()
						.withDetail("responseBody", result);
			}
		} catch (Exception ex) {
			builder.down(ex);
		} catch (Throwable throwable) {
			builder.down().withException(new Exception(throwable));
		}
	}
}
