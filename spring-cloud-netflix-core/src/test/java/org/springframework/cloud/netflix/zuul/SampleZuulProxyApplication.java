package org.springframework.cloud.netflix.zuul;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
@EnableZuulProxy
@EnableEurekaClient
public class SampleZuulProxyApplication {

    @RequestMapping("/testing123")
    public String testing123() {
        throw new RuntimeException("myerror");
    }

    @RequestMapping("/local/self")
    public String local() {
        return "Hello local";
    }

    @RequestMapping(value="/local/self/{id}", method=RequestMethod.DELETE)
    public String delete() {
        return "Deleted!";
    }

    @RequestMapping(value="/local/self/{id}", method=RequestMethod.GET)
    public String get() {
        return "Gotten!";
    }

	@RequestMapping("/")
	public String home() {
		return "Hello world";
	}
	
	public static void main(String[] args) {
		SpringApplication.run(SampleZuulProxyApplication.class, args);
	}

}
