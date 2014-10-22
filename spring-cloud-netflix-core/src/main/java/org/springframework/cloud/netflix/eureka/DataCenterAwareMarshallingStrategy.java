/*
 * Copyright 2013-2014 the original author or authors.
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

import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.UniqueIdentifier;
import com.netflix.appinfo.DataCenterInfo.Name;
import com.netflix.appinfo.InstanceInfo;
import com.thoughtworks.xstream.MarshallingStrategy;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.converters.DataHolder;
import com.thoughtworks.xstream.core.TreeMarshallingStrategy;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

/**
 * A special purpose wrapper for an XStream TreeMarshallingStrategy that is aware of the
 * {@link InstanceInfo} type and can create a more useful {@link DataCenterInfo} for it
 * after unmarshalling. If the InstanceInfo has a metadataMap containing an
 * <code>instanceId</code>, and the DataCenterInfo is not already an AmazonInfo, then the
 * instanceId is used to create an identifier, but appending it to the existing one. This
 * is useful when not running Eureka in bare EC2 VMs, so the EC2 metadata is not available
 * for uniquely identifying the InstanceInfo (the default is to just use the hostname, but
 * that isn't very useful when sitting behind a proxy).
 * 
 * @author Dave Syer
 *
 */
public class DataCenterAwareMarshallingStrategy implements MarshallingStrategy {

	private TreeMarshallingStrategy delegate = new TreeMarshallingStrategy();

	@Override
	public Object unmarshal(Object root, HierarchicalStreamReader reader,
			DataHolder dataHolder, ConverterLookup converterLookup, Mapper mapper) {
		Object obj = delegate
				.unmarshal(root, reader, dataHolder, converterLookup, mapper);
		if (obj instanceof InstanceInfo) {
			InstanceInfo info = (InstanceInfo) obj;
			String instanceId = info.getMetadata().get("instanceId");
			DataCenterInfo dataCenter = info.getDataCenterInfo();
			if (instanceId != null && Name.Amazon != dataCenter.getName()) {
				String old = info.getId();
				info = new InstanceInfo.Builder(info).setDataCenterInfo(
						new InstanceIdDataCenterInfo(old + ":" + instanceId)).build();
				obj = info;
			}
		}
		return obj;
	}

	@Override
	public void marshal(HierarchicalStreamWriter writer, Object obj,
			ConverterLookup converterLookup, Mapper mapper, DataHolder dataHolder) {
		delegate.marshal(writer, obj, converterLookup, mapper, dataHolder);
	}
	
	public static class InstanceIdDataCenterInfo implements DataCenterInfo, UniqueIdentifier {

		private String instanceId;

		public InstanceIdDataCenterInfo(String instanceId) {
			this.instanceId = instanceId;
		}

		@Override
		public Name getName() {
			return Name.MyOwn;
		}

		@Override
		public String getId() {
			return instanceId;
		}

	}

}
