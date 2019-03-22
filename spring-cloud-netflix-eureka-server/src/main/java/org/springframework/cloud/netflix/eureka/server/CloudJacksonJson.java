/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.eureka.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.function.Supplier;

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
import com.netflix.discovery.converters.EurekaJacksonCodec.InstanceInfoDeserializer;
import com.netflix.discovery.converters.EurekaJacksonCodec.InstanceInfoSerializer;
import com.netflix.discovery.converters.wrappers.CodecWrappers.LegacyJacksonJson;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;

import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import static com.netflix.discovery.converters.wrappers.CodecWrappers.getCodecName;

/**
 * @author Spencer Gibb
 */
public class CloudJacksonJson extends LegacyJacksonJson {

	protected final CloudJacksonCodec codec = new CloudJacksonCodec();

	public CloudJacksonCodec getCodec() {
		return codec;
	}

	@Override
	public String codecName() {
		return getCodecName(LegacyJacksonJson.class);
	}

	@Override
	public <T> String encode(T object) throws IOException {
		return this.codec.writeToString(object);
	}

	@Override
	public <T> void encode(T object, OutputStream outputStream) throws IOException {
		this.codec.writeTo(object, outputStream);
	}

	@Override
	public <T> T decode(String textValue, Class<T> type) throws IOException {
		return this.codec.readValue(type, textValue);
	}

	@Override
	public <T> T decode(InputStream inputStream, Class<T> type) throws IOException {
		return this.codec.readValue(type, inputStream);
	}

	static InstanceInfo updateIfNeeded(final InstanceInfo info) {
		if (info.getInstanceId() == null && info.getMetadata() != null) {
			String instanceId = info.getMetadata().get("instanceId");
			if (StringUtils.hasText(instanceId)) {
				// backwards compatibility for Angel
				if (StringUtils.hasText(info.getHostName())
						&& !instanceId.startsWith(info.getHostName())) {
					instanceId = info.getHostName() + ":" + instanceId;
				}
				return new InstanceInfo.Builder(info).setInstanceId(instanceId).build();
			}
		}
		return info;
	}

	static class CloudJacksonCodec extends EurekaJacksonCodec {

		private static final Version VERSION = new Version(1, 1, 0, null, null, null);

		@SuppressWarnings("deprecation")
		CloudJacksonCodec() {
			super();

			ObjectMapper mapper = new ObjectMapper();
			mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

			SimpleModule module = new SimpleModule("eureka1.x", VERSION);
			module.addSerializer(DataCenterInfo.class, new DataCenterInfoSerializer());
			module.addSerializer(InstanceInfo.class, new CloudInstanceInfoSerializer());
			module.addSerializer(Application.class, new ApplicationSerializer());
			module.addSerializer(Applications.class, new ApplicationsSerializer(
					this.getVersionDeltaKey(), this.getAppHashCodeKey()));

			// TODO: Watch if this causes problems
			// module.addDeserializer(DataCenterInfo.class,
			// new DataCenterInfoDeserializer());
			module.addDeserializer(LeaseInfo.class, new LeaseInfoDeserializer());
			module.addDeserializer(InstanceInfo.class,
					new CloudInstanceInfoDeserializer(mapper));
			module.addDeserializer(Application.class,
					new ApplicationDeserializer(mapper));
			module.addDeserializer(Applications.class, new ApplicationsDeserializer(
					mapper, this.getVersionDeltaKey(), this.getAppHashCodeKey()));

			mapper.registerModule(module);

			HashMap<Class<?>, Supplier<ObjectReader>> readers = new HashMap<>();
			readers.put(InstanceInfo.class, () -> mapper.reader()
					.withType(InstanceInfo.class).withRootName("instance"));
			readers.put(Application.class, () -> mapper.reader()
					.withType(Application.class).withRootName("application"));
			readers.put(Applications.class, () -> mapper.reader()
					.withType(Applications.class).withRootName("applications"));
			setField("objectReaderByClass", readers);

			HashMap<Class<?>, ObjectWriter> writers = new HashMap<>();
			writers.put(InstanceInfo.class, mapper.writer().withType(InstanceInfo.class)
					.withRootName("instance"));
			writers.put(Application.class, mapper.writer().withType(Application.class)
					.withRootName("application"));
			writers.put(Applications.class, mapper.writer().withType(Applications.class)
					.withRootName("applications"));
			setField("objectWriterByClass", writers);

			setField("mapper", mapper);
		}

		void setField(String name, Object value) {
			Field field = ReflectionUtils.findField(EurekaJacksonCodec.class, name);
			ReflectionUtils.makeAccessible(field);
			ReflectionUtils.setField(field, this, value);
		}

	}

	static class CloudInstanceInfoSerializer extends InstanceInfoSerializer {

		@Override
		public void serialize(final InstanceInfo info, JsonGenerator jgen,
				SerializerProvider provider) throws IOException {

			InstanceInfo updated = updateIfNeeded(info);
			super.serialize(updated, jgen, provider);
		}

	}

	static class CloudInstanceInfoDeserializer extends InstanceInfoDeserializer {

		protected CloudInstanceInfoDeserializer(ObjectMapper mapper) {
			super(mapper);
		}

		@Override
		public InstanceInfo deserialize(JsonParser jp, DeserializationContext context)
				throws IOException {
			InstanceInfo info = super.deserialize(jp, context);
			InstanceInfo updated = updateIfNeeded(info);
			return updated;
		}

	}

}
