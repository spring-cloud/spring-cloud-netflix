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

package org.springframework.cloud.netflix.turbine.stream;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.apachecommons.CommonsLog;
import rx.subjects.PublishSubject;

/**
 * @author Spencer Gibb
 */
@CommonsLog
@Component // needed for ServiceActivator to be picked up
public class HystrixStreamAggregator {

	private ObjectMapper objectMapper;

	private PublishSubject<Map<String, Object>> subject;

	@Autowired
	public HystrixStreamAggregator(ObjectMapper objectMapper,
			PublishSubject<Map<String, Object>> subject) {
		this.objectMapper = objectMapper;
		this.subject = subject;
	}

	@ServiceActivator(inputChannel = TurbineStreamClient.INPUT)
	public void sendToSubject(@Payload String payload) {
		if (payload.startsWith("\"")) {
			// Legacy payload from an Angel client
			payload = payload.substring(1, payload.length() - 1);
			payload = payload.replace("\\\"", "\"");
		}
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = this.objectMapper.readValue(payload, Map.class);
			Map<String, Object> data = getPayloadData(map);

			log.debug("Received hystrix stream payload: " + data);
			this.subject.onNext(data);
		}
		catch (IOException ex) {
			log.error("Error receiving hystrix stream payload: " + payload, ex);
		}
	}

	public static Map<String, Object> getPayloadData(Map<String, Object> jsonMap) {
		@SuppressWarnings("unchecked")
		Map<String, Object> origin = (Map<String, Object>) jsonMap.get("origin");
		String instanceId = null;
		if (origin.containsKey("id")) {
			instanceId = origin.get("id").toString();
		}
		if (!StringUtils.hasText(instanceId)) {
			// TODO: instanceid template
			instanceId = origin.get("serviceId") + ":" + origin.get("host") + ":"
					+ origin.get("port");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> data = (Map<String, Object>) jsonMap.get("data");
		data.put("instanceId", instanceId);
		return data;
	}

}
