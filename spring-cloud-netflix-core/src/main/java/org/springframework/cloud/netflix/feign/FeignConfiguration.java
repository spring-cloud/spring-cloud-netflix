package org.springframework.cloud.netflix.feign;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.archaius.ConfigurableEnvironmentConfiguration;
import org.springframework.context.annotation.Configuration;

import feign.Client;
import feign.Contract;
import feign.Feign;
import feign.Logger;
import feign.Request;
import feign.Retryer;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import feign.ribbon.LoadBalancingTarget;

/**
 * @author Spencer Gibb
 */
@Configuration
public class FeignConfiguration {
	@Autowired
	ConfigurableEnvironmentConfiguration envConfig; // FIXME: howto enforce this?

	@Autowired
	Decoder decoder;

	@Autowired
	Encoder encoder;

	@Autowired
	Logger logger;

	@Autowired
	Contract contract;

	@Autowired(required = false)
	Logger.Level logLevel;

	@Autowired(required = false)
	Retryer retryer;

	@Autowired(required = false)
	ErrorDecoder errorDecoder;

	@Autowired(required = false)
	Request.Options options;

	@Autowired(required = false)
	Client ribbonClient;

	protected Feign.Builder feign() {
		Feign.Builder builder = Feign.builder()
				// required values
				.logger(this.logger).encoder(this.encoder).decoder(this.decoder)
				.contract(this.contract);

		// optional values
		if (this.logLevel != null) {
			builder.logLevel(this.logLevel);
		}
		if (this.retryer != null) {
			builder.retryer(this.retryer);
		}
		if (this.errorDecoder != null) {
			builder.errorDecoder(this.errorDecoder);
		}
		if (this.options != null) {
			builder.options(this.options);
		}

		return builder;
	}

	protected <T> T loadBalance(Class<T> type, String schemeName) {
		return loadBalance(feign(), type, schemeName);
	}

	protected <T> T loadBalance(Feign.Builder builder, Class<T> type, String schemeName) {
		if (this.ribbonClient != null) {
			return builder.client(this.ribbonClient).target(type, schemeName);
		}
		else {
			return builder.target(LoadBalancingTarget.create(type, schemeName));
		}
	}

}
