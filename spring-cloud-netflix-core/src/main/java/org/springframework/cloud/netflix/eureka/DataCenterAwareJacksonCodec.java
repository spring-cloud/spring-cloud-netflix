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

package org.springframework.cloud.netflix.eureka;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

import lombok.SneakyThrows;
import lombok.extern.apachecommons.CommonsLog;

import org.springframework.util.ReflectionUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.LeaseInfo;
import com.netflix.discovery.converters.EurekaJacksonCodec;
import com.netflix.discovery.converters.StringCache;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;

/**
 * @author Spencer Gibb
 */
public class DataCenterAwareJacksonCodec extends EurekaJacksonCodec {
	private static final Version VERSION = new Version(1, 1, 0, null);

	@SneakyThrows
	public DataCenterAwareJacksonCodec() {
		super();

		ObjectMapper mapper = new ObjectMapper();

		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

		SimpleModule module = new SimpleModule("eureka1.x", VERSION);
		module.addSerializer(DataCenterInfo.class, new DataCenterInfoSerializer());
		module.addSerializer(InstanceInfo.class, new DCAwareInstanceInfoSerializer());
		module.addSerializer(Application.class, new ApplicationSerializer());
		module.addSerializer(Applications.class, new ApplicationsSerializer(getVersionDeltaKey(), getAppHashCodeKey()));

		module.addDeserializer(DataCenterInfo.class, new DataCenterInfoDeserializer(getCache()));
		module.addDeserializer(LeaseInfo.class, new LeaseInfoDeserializer());
		module.addDeserializer(InstanceInfo.class, new DCAwareInstanceInfoDeserializer(getCache()));
		module.addDeserializer(Application.class, new ApplicationDeserializer(mapper, getCache()));
		module.addDeserializer(Applications.class, new ApplicationsDeserializer(mapper, getVersionDeltaKey(), getAppHashCodeKey()));

		mapper.registerModule(module);

		Map<Class<?>, ObjectReader> readers = getField("objectReaderByClass");
		readers.put(InstanceInfo.class, mapper.reader().withType(InstanceInfo.class).withRootName("instance"));
		readers.put(Application.class, mapper.reader().withType(Application.class).withRootName("application"));
		readers.put(Applications.class, mapper.reader().withType(Applications.class).withRootName("applications"));

		Map<Class<?>, ObjectWriter> writers = getField("objectWriterByClass");
		writers.put(InstanceInfo.class, mapper.writer().withType(InstanceInfo.class).withRootName("instance"));
		writers.put(Application.class, mapper.writer().withType(Application.class).withRootName("application"));
		writers.put(Applications.class, mapper.writer().withType(Applications.class).withRootName("applications"));

		Field field = ReflectionUtils.findField(EurekaJacksonCodec.class, "mapper");
		ReflectionUtils.makeAccessible(field);
		field.set(this, mapper);
	}

	private <T> T getField(String name) throws IllegalAccessException {
		Field field = ReflectionUtils.findField(EurekaJacksonCodec.class, name);
		ReflectionUtils.makeAccessible(field);
		return (T) field.get(this);
	}

	@SneakyThrows
	public static void init() {
		if (!(EurekaJacksonCodec.getInstance() instanceof DataCenterAwareJacksonCodec)) {
			INSTANCE = new DataCenterAwareJacksonCodec();
		}
	}

	@CommonsLog
	private static class DCAwareInstanceInfoSerializer extends
			InstanceInfoSerializer {

		@Override
		public void serialize(InstanceInfo info, JsonGenerator jgen,
				SerializerProvider provider) throws IOException {
			String instanceId = info.getMetadata().get("instanceId");
			DataCenterInfo dataCenter = info.getDataCenterInfo();
			if (instanceId != null
					&& DataCenterInfo.Name.Amazon != dataCenter.getName()) {
				String old = info.getId();
				String id = old.endsWith(instanceId) ? old : old + ":" + instanceId;
				info = new InstanceInfo.Builder(info).setDataCenterInfo(
						new InstanceIdDataCenterInfo(id)).build();
			}

			super.serialize(info, jgen, provider);
		}
	}

	private class DCAwareInstanceInfoDeserializer extends InstanceInfoDeserializer {
		private DCAwareInstanceInfoDeserializer(StringCache cache) {
			super(getMapper(), cache);
		}

		@Override
		public InstanceInfo deserialize(JsonParser jp, DeserializationContext context)
				throws IOException {
			InstanceInfo info = super.deserialize(jp, context);
			String instanceId = info.getMetadata().get("instanceId");
			DataCenterInfo dataCenter = info.getDataCenterInfo();
			if (instanceId != null && DataCenterInfo.Name.Amazon != dataCenter.getName()) {
				String old = info.getId();
				String id = old.endsWith(instanceId) ? old : old + ":" + instanceId;
				info = new InstanceInfo.Builder(info).setDataCenterInfo(
						new InstanceIdDataCenterInfo(id)).build();
			}
			return info;
		}
	}

}
