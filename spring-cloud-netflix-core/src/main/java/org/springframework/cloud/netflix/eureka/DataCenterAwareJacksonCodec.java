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
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.SneakyThrows;
import lombok.extern.apachecommons.CommonsLog;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.LeaseInfo;
import com.netflix.discovery.converters.Auto;
import com.netflix.discovery.converters.EurekaJacksonCodec;
import com.netflix.discovery.converters.StringCache;

/**
 * @author Spencer Gibb
 */
public class DataCenterAwareJacksonCodec extends EurekaJacksonCodec {
	private static final Version VERSION = new Version(1, 1, 0, null);

	@SneakyThrows
	public DataCenterAwareJacksonCodec() {
		super();
		SimpleModule module = new SimpleModule("spring-cloud-eureka1.x", VERSION);
		module.addSerializer(InstanceInfo.class, new DCAwareInstanceInfoSerializer());
		module.addDeserializer(InstanceInfo.class, new DCAwareInstanceInfoDeserializer(
				new StringCache()));
		getMapper().registerModule(module);
	}

	@SneakyThrows
	public static void init() {
		if (!(EurekaJacksonCodec.getInstance() instanceof DataCenterAwareJacksonCodec)) {
			INSTANCE = new DataCenterAwareJacksonCodec();
		}
	}

	@CommonsLog
	private static class DCAwareInstanceInfoSerializer extends
			JsonSerializer<InstanceInfo> {
		// For backwards compatibility
		private static final Object EMPTY_METADATA = Collections.singletonMap("@class",
				"java.util.Collections$EmptyMap");

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

			jgen.writeStartObject();

			jgen.writeStringField(ELEM_HOST, info.getHostName());
			jgen.writeStringField(ELEM_APP, info.getAppName());
			jgen.writeStringField(ELEM_IP, info.getIPAddr());

			if (!("unknown".equals(info.getSID()) || "na".equals(info.getSID()))) {
				jgen.writeStringField(ELEM_SID, info.getSID());
			}

			jgen.writeStringField(ELEM_STATUS, info.getStatus().name());
			jgen.writeStringField(ELEM_OVERRIDDEN_STATUS, info.getOverriddenStatus()
					.name());

			jgen.writeFieldName(ELEM_PORT);
			jgen.writeStartObject();
			jgen.writeNumberField("$", info.getPort());
			jgen.writeStringField("@enabled",
					Boolean.toString(info.isPortEnabled(InstanceInfo.PortType.UNSECURE)));
			jgen.writeEndObject();

			jgen.writeFieldName(ELEM_SECURE_PORT);
			jgen.writeStartObject();
			jgen.writeNumberField("$", info.getSecurePort());
			jgen.writeStringField("@enabled",
					Boolean.toString(info.isPortEnabled(InstanceInfo.PortType.SECURE)));
			jgen.writeEndObject();

			jgen.writeNumberField(ELEM_COUNTRY_ID, info.getCountryId());

			if (info.getDataCenterInfo() != null) {
				jgen.writeObjectField(NODE_DATACENTER, info.getDataCenterInfo());
			}
			if (info.getLeaseInfo() != null) {
				jgen.writeObjectField(NODE_LEASE, info.getLeaseInfo());
			}

			Map<String, String> metadata = info.getMetadata();
			if (metadata != null) {
				if (metadata.isEmpty()) {
					jgen.writeObjectField(NODE_METADATA, EMPTY_METADATA);
				}
				else {
					jgen.writeObjectField(NODE_METADATA, metadata);
				}
			}
			autoMarshalEligible(info, jgen);

			jgen.writeEndObject();
		}

		private void autoMarshalEligible(Object o, JsonGenerator jgen) {
			try {
				Class c = o.getClass();
				Field[] fields = c.getDeclaredFields();
				Annotation annotation;
				for (Field f : fields) {
					annotation = f.getAnnotation(Auto.class);
					if (annotation != null) {
						f.setAccessible(true);
						if (f.get(o) != null) {
							jgen.writeStringField(f.getName(), String.valueOf(f.get(o)));
						}

					}
				}
			}
			catch (Throwable th) {
				log.error("Error in marshalling the object", th);
			}
		}
	}

	private class DCAwareInstanceInfoDeserializer extends JsonDeserializer<InstanceInfo> {
		private final StringCache cache;

		private DCAwareInstanceInfoDeserializer(StringCache cache) {
			this.cache = cache;
		}

		@Override
		public InstanceInfo deserialize(JsonParser jp, DeserializationContext context)
				throws IOException {
			InstanceInfo.Builder builder = InstanceInfo.Builder.newBuilder();

			JsonNode node = jp.getCodec().readTree(jp);

			/**
			 * These are set via single call to
			 * {@link com.netflix.appinfo.InstanceInfo.Builder#setHealthCheckUrlsForDeser(String, String, String)}
			 * .
			 */
			String healthChecUrl = null;
			String healthCheckSecureUrl = null;

			Iterator<String> fieldNames = node.fieldNames();
			while (fieldNames.hasNext()) {
				String fieldName = fieldNames.next();
				JsonNode fieldNode = node.get(fieldName);

				if (!fieldNode.isNull()) {
					if (ELEM_HOST.equals(fieldName)) {
						builder.setHostName(fieldNode.asText());
					}
					else if (ELEM_APP.equals(fieldName)) {
						builder.setAppName(cache.cachedValueOf(fieldNode.asText()));
					}
					else if (ELEM_IP.equals(fieldName)) {
						builder.setIPAddr(fieldNode.asText());
					}
					else if (ELEM_SID.equals(fieldName)) {
						builder.setSID(cache.cachedValueOf(fieldNode.asText()));
					}
					else if (ELEM_IDENTIFYING_ATTR.equals(fieldName)) {
						// nothing;
					}
					else if (ELEM_STATUS.equals(fieldName)) {
						builder.setStatus(InstanceInfo.InstanceStatus.toEnum(fieldNode
								.asText()));
					}
					else if (ELEM_OVERRIDDEN_STATUS.equals(fieldName)) {
						builder.setOverriddenStatus(InstanceInfo.InstanceStatus
								.toEnum(fieldNode.asText()));
					}
					else if (ELEM_PORT.equals(fieldName)) {
						int port = fieldNode.get("$").asInt();
						boolean enabled = fieldNode.get("@enabled").asBoolean();
						builder.setPort(port);
						builder.enablePort(InstanceInfo.PortType.UNSECURE, enabled);
					}
					else if (ELEM_SECURE_PORT.equals(fieldName)) {
						int port = fieldNode.get("$").asInt();
						boolean enabled = fieldNode.get("@enabled").asBoolean();
						builder.setSecurePort(port);
						builder.enablePort(InstanceInfo.PortType.SECURE, enabled);
					}
					else if (ELEM_COUNTRY_ID.equals(fieldName)) {
						builder.setCountryId(Integer.valueOf(fieldNode.asText())
								.intValue());
					}
					else if (NODE_DATACENTER.equals(fieldName)) {
						builder.setDataCenterInfo(getMapper().treeToValue(fieldNode,
								DataCenterInfo.class));
					}
					else if (NODE_LEASE.equals(fieldName)) {
						builder.setLeaseInfo(getMapper().treeToValue(fieldNode,
								LeaseInfo.class));
					}
					else if (NODE_METADATA.equals(fieldName)) {
						Map<String, String> meta = null;
						Iterator<String> metaNameIt = fieldNode.fieldNames();
						while (metaNameIt.hasNext()) {
							String key = cache.cachedValueOf(metaNameIt.next());
							if (key.equals("@class")) { // For backwards compatibility
								if (meta == null && !metaNameIt.hasNext()) { // Optimize
																				// for
																				// empty
																				// maps
									meta = Collections.emptyMap();
								}
							}
							else {
								if (meta == null) {
									meta = new ConcurrentHashMap<String, String>();
								}
								String value = cache.cachedValueOf(fieldNode.get(key)
										.asText());
								meta.put(key, value);
							}
						}
						builder.setMetadata(meta);
					}
					else if (ELEM_HEALTHCHECKURL.equals(fieldName)) {
						healthChecUrl = fieldNode.asText();
					}
					else if (ELEM_SECHEALTHCHECKURL.equals(fieldName)) {
						healthCheckSecureUrl = fieldNode.asText();
					}
					else if (ELEM_APPGROUPNAME.equals(fieldName)) {
						builder.setAppGroupName(fieldNode.asText());
					}
					else if (ELEM_HOMEPAGEURL.equals(fieldName)) {
						builder.setHomePageUrlForDeser(fieldNode.asText());
					}
					else if (ELEM_STATUSPAGEURL.equals(fieldName)) {
						builder.setStatusPageUrlForDeser(fieldNode.asText());
					}
					else if (ELEM_VIPADDRESS.equals(fieldName)) {
						builder.setVIPAddressDeser(fieldNode.asText());
					}
					else if (ELEM_SECVIPADDRESS.equals(fieldName)) {
						builder.setSecureVIPAddressDeser(fieldNode.asText());
					}
					else if (ELEM_ISCOORDINATINGDISCSOERVER.equals(fieldName)) {
						builder.setIsCoordinatingDiscoveryServer(fieldNode.asBoolean());
					}
					else if (ELEM_LASTUPDATEDTS.equals(fieldName)) {
						builder.setLastUpdatedTimestamp(fieldNode.asLong());
					}
					else if (ELEM_LASTDIRTYTS.equals(fieldName)) {
						builder.setLastDirtyTimestamp(fieldNode.asLong());
					}
					else if (ELEM_ACTIONTYPE.equals(fieldName)) {
						builder.setActionType(InstanceInfo.ActionType.valueOf(fieldNode
								.asText()));
					}
					else if (ELEM_ASGNAME.equals(fieldName)) {
						builder.setASGName(fieldNode.asText());
					}
					else {
						autoUnmarshalEligible(fieldName, fieldNode.asText(),
								builder.getRawInstance());
					}
				}
			}
			builder.setHealthCheckUrlsForDeser(healthChecUrl, healthCheckSecureUrl);

			InstanceInfo info = builder.build();
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

		private void autoUnmarshalEligible(String fieldName, String value, Object o) {
			try {
				Class c = o.getClass();
				Field f = null;
				try {
					f = c.getDeclaredField(fieldName);
				}
				catch (NoSuchFieldException e) {
					// TODO XStream version increments metrics counter here
				}
				if (f == null) {
					return;
				}
				Annotation annotation = f.getAnnotation(Auto.class);
				if (annotation == null) {
					return;
				}
				f.setAccessible(true);

				Class returnClass = f.getType();
				if (value != null) {
					if (!String.class.equals(returnClass)) {
						Method method = returnClass.getDeclaredMethod("valueOf",
								java.lang.String.class);
						Object valueObject = method.invoke(returnClass, value);
						f.set(o, valueObject);
					}
					else {
						f.set(o, value);

					}
				}
			}
			catch (Throwable th) {
				// log.error("Error in unmarshalling the object:", th);
			}
		}

	}

}
