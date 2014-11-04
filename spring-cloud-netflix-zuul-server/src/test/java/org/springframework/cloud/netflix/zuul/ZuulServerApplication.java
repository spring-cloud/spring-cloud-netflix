package org.springframework.cloud.netflix.zuul;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
@EnableZuulServer
public class ZuulServerApplication {

	public static void main(String[] args) {
		new SpringApplicationBuilder(ZuulServerApplication.class).web(true).run(args);
	}

}
