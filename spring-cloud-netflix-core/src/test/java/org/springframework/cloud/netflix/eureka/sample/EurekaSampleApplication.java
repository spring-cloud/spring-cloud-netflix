package org.springframework.cloud.netflix.eureka.sample;

import org.springframework.boot.actuate.metrics.repository.InMemoryMetricRepository;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.appinfo.InstanceInfo;

@Configuration
@ComponentScan
@EnableAutoConfiguration
@RestController
@EnableEurekaClient
public class EurekaSampleApplication {

	@Bean
	public InMemoryMetricRepository inMemoryMetricRepository() {
		return new InMemoryMetricRepository();
	}

	@Bean
	public HealthCheckHandler healthCheckHandler() {
		return new HealthCheckHandler() {
			@Override
			public InstanceInfo.InstanceStatus getStatus(
					InstanceInfo.InstanceStatus currentStatus) {
				return InstanceInfo.InstanceStatus.UP;
			}
		};
	}

	@RequestMapping("/")
	public String home() {
		return "Hello world";
	}

	public static void main(String[] args) {
		new SpringApplicationBuilder(EurekaSampleApplication.class).web(true).run(args);
	}

}
