/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.archaius;

import com.netflix.config.DynamicConfiguration;
import com.netflix.config.FixedDelayPollingScheduler;
import com.netflix.config.PolledConfigurationSource;
import com.netflix.config.sources.JDBCConfigurationSource;
import org.apache.commons.configuration.AbstractConfiguration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * @author Alexandru-George Burghelea
 */
@Configuration
public class ArchaiusExternalConfiguration {

	@Bean
	@Qualifier("dynamicConfiguration")
	public AbstractConfiguration createDynamicConfiguration() {
		PolledConfigurationSource source = new JDBCConfigurationSource(initDataSource(),
				"select distinct property_key, property_value from properties",
				"property_key", "property_value");
		return new DynamicConfiguration(source, new FixedDelayPollingScheduler(0, 1000, false));
	}

	@Bean
	@Qualifier("dataSource")
	public SingleConnectionDataSource initDataSource() {
		SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
		dataSource.setDriverClassName("org.h2.Driver");
		dataSource
				.setUrl("jdbc:h2:mem:test_archaius;AUTOCOMMIT=ON;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;INIT=RUNSCRIPT FROM 'classpath:archaius_db_store.sql'");
		dataSource.setUsername("sa");
		dataSource.setPassword("");
		dataSource.setSuppressClose(true);
		return dataSource;
	}

}
