/*
 * Copyright 2016-2017 the original author or authors.
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
package org.springframework.cloud.netflix.eureka.server.doc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.ws.rs.core.MediaType;

import com.jayway.restassured.mapper.ObjectMapperDeserializationContext;
import com.jayway.restassured.mapper.ObjectMapperSerializationContext;
import com.netflix.discovery.converters.EntityBodyConverter;

final class EurekaObjectMapper
		implements com.jayway.restassured.mapper.ObjectMapper {
	private EntityBodyConverter converter = new EntityBodyConverter();

	@Override
	public Object serialize(ObjectMapperSerializationContext context) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			converter.write(context.getObjectToSerialize(), out,
					MediaType.APPLICATION_JSON_TYPE);
		}
		catch (IOException e) {
			throw new IllegalStateException("Cannot serialize", e);
		}
		return out.toByteArray();
	}

	@Override
	public Object deserialize(
			ObjectMapperDeserializationContext context) {
		try {
			return converter.read(
					context.getDataToDeserialize().asInputStream(),
					context.getType(), MediaType.APPLICATION_JSON_TYPE);
		}
		catch (IOException e) {
			throw new IllegalStateException("Cannot deserialize", e);
		}
	}
}