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
import com.netflix.appinfo.DataCenterInfo.Name;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.UniqueIdentifier;
import com.netflix.discovery.converters.Converters.ApplicationsConverter;
import com.netflix.discovery.converters.Converters.InstanceInfoConverter;
import com.netflix.discovery.converters.StringCache;
import com.netflix.discovery.shared.Applications;
import com.thoughtworks.xstream.MarshallingStrategy;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.converters.DataHolder;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
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
 */
public class DataCenterAwareMarshallingStrategy implements MarshallingStrategy {

	private TreeMarshallingStrategy delegate = new TreeMarshallingStrategy();

	public DataCenterAwareMarshallingStrategy() {
	}

	@Override
	public Object unmarshal(Object root, HierarchicalStreamReader reader,
			DataHolder dataHolder, ConverterLookup converterLookup, Mapper mapper) {
		ConverterLookup wrapped = new DataCenterAwareConverterLookup(converterLookup);
		return this.delegate.unmarshal(root, reader, dataHolder, wrapped, mapper);
	}

	@Override
	public void marshal(HierarchicalStreamWriter writer, Object obj,
			ConverterLookup converterLookup, Mapper mapper, DataHolder dataHolder) {
		ConverterLookup wrapped = new DataCenterAwareConverterLookup(converterLookup);
		this.delegate.marshal(writer, obj, wrapped, mapper, dataHolder);
	}

	public static class InstanceIdDataCenterInfo implements DataCenterInfo,
			UniqueIdentifier {

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
			return this.instanceId;
		}

	}

	private static class DataCenterAwareConverterLookup implements ConverterLookup {

		private ConverterLookup delegate;

		public DataCenterAwareConverterLookup(ConverterLookup delegate) {
			this.delegate = delegate;
		}

		@Override
		public Converter lookupConverterForType(@SuppressWarnings("rawtypes") Class type) {
			Converter converter = this.delegate.lookupConverterForType(type);
			if (InstanceInfo.class == type) {
				return new DataCenterAwareConverter();
			}
			else if (Applications.class == type) {
				return new ApplicationsConverter();
			}
			return converter;
		}

	}

	private static class DataCenterAwareConverter extends InstanceInfoConverter {

		public DataCenterAwareConverter() {
			super(new StringCache());
		}

		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer,
				MarshallingContext context) {
			InstanceInfo info = (InstanceInfo) source;
			String instanceId = info.getMetadata().get("instanceId");
			DataCenterInfo dataCenter = info.getDataCenterInfo();
			if (instanceId != null && Name.Amazon != dataCenter.getName()) {
				String old = info.getId();
				String id = old.endsWith(instanceId) ? old : old + ":" + instanceId;
				info = new InstanceInfo.Builder(info).setDataCenterInfo(
						new InstanceIdDataCenterInfo(id)).build();
				source = info;
			}
			super.marshal(source, writer, context);
		}

		@Override
		public Object unmarshal(HierarchicalStreamReader reader,
				UnmarshallingContext context) {
			Object obj = super.unmarshal(reader, context);
			InstanceInfo info = (InstanceInfo) obj;
			String instanceId = info.getMetadata().get("instanceId");
			DataCenterInfo dataCenter = info.getDataCenterInfo();
			if (instanceId != null && Name.Amazon != dataCenter.getName()) {
				String old = info.getId();
				String id = old.endsWith(instanceId) ? old : old + ":" + instanceId;
				info = new InstanceInfo.Builder(info).setDataCenterInfo(
						new InstanceIdDataCenterInfo(id)).build();
				obj = info;
			}
			return obj;
		}

	}

}
