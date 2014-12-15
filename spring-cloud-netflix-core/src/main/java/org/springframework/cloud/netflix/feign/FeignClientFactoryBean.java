package org.springframework.cloud.netflix.feign;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.springframework.beans.factory.FactoryBean;

/**
* @author Spencer Gibb
*/
@Data
@EqualsAndHashCode(callSuper=false)
class FeignClientFactoryBean extends FeignConfiguration implements FactoryBean<Object> {

	private boolean loadbalance;
	private Class<?> type;
	private String schemeName;

	@Override
	public Object getObject() throws Exception {
		if (!schemeName.startsWith("http")) {
			schemeName = "http://"+schemeName;
		}
		if (loadbalance) {
			return loadBalance(type, schemeName);
		}
		return feign().target(type, schemeName);
	}

	@Override
	public Class<?> getObjectType() {
		return type;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
