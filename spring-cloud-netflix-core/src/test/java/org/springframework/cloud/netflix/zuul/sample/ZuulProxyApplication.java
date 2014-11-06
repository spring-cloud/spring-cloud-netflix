package org.springframework.cloud.netflix.zuul.sample;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Configuration
@ComponentScan
@EnableAutoConfiguration
@RestController
@EnableZuulProxy
public class ZuulProxyApplication {

    @RequestMapping("/testing123")
    public String testing123() {
        throw new RuntimeException("myerror");
    }

	@RequestMapping("/")
	public String home() {
		return "Hello world";
	}
	
	public static void main(String[] args) {
		new SpringApplicationBuilder(ZuulProxyApplication.class).web(true).run(args);
	}

}
