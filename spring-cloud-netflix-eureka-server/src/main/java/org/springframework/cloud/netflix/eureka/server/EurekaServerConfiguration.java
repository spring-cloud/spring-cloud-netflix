package org.springframework.cloud.netflix.eureka.server;

import javax.servlet.Filter;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.eureka.EurekaServerConfigBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.google.common.collect.Lists;
import com.sun.jersey.spi.container.servlet.ServletContainer;

/**
 *
 * @author Gunnar Hillert
 *
 */
@Configuration
@Import(EurekaServerInitializerConfiguration.class)
@EnableEurekaClient
public class EurekaServerConfiguration extends WebMvcConfigurerAdapter {
	
	@Bean
	public EurekaController eurekaController() {
		return new EurekaController();
	}

	@Bean
	public FilterRegistrationBean jersey() {
		FilterRegistrationBean bean = new FilterRegistrationBean();
		bean.setFilter(new ServletContainer());
		bean.setOrder(Ordered.LOWEST_PRECEDENCE);
		bean.addInitParameter("com.sun.jersey.config.property.packages",
				"com.netflix.discovery;com.netflix.eureka");
		bean.addInitParameter("com.sun.jersey.config.feature.FilterContextPath", EurekaServerConfigBean.DEFAULT_PREFIX);
		bean.setUrlPatterns(Lists.newArrayList(EurekaServerConfigBean.DEFAULT_PATH + "/*"));
		return bean;
	}

	// TODO: remove this when we upgrade to Boot 1.1.6
	@Bean
	public FilterRegistrationBean metricFilterRegistration(@Qualifier("metricFilter") Filter filter) {
		FilterRegistrationBean bean = new FilterRegistrationBean();
		bean.setFilter(filter);
		bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
		return bean;
	}

	// TODO: remove this when we upgrade to Boot 1.1.6
	@Bean
	public FilterRegistrationBean traceFilterRegistration(@Qualifier("webRequestLoggingFilter") Filter filter) {
		FilterRegistrationBean bean = new FilterRegistrationBean();
		bean.setFilter(filter);
		bean.setOrder(Ordered.LOWEST_PRECEDENCE - 10);
		return bean;
	}

}
