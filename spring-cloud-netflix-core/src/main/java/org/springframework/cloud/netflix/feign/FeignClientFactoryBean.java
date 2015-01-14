package org.springframework.cloud.netflix.feign;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.springframework.beans.factory.FactoryBean;

/**
 * @author Spencer Gibb
 */
@Data
@EqualsAndHashCode(callSuper = false)
class FeignClientFactoryBean extends FeignConfiguration implements FactoryBean<Object> {

	private boolean loadbalance;
	private Class<?> type;
	private String schemeName;

	@Override
	public Object getObject() throws Exception {
		if (!this.schemeName.startsWith("http")) {
			this.schemeName = "http://" + this.schemeName;
		}
		if (this.loadbalance) {
			return loadBalance(this.type, this.schemeName);
		}
		return feign().target(this.type, this.schemeName);
	}

	@Override
	public Class<?> getObjectType() {
		return this.type;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
