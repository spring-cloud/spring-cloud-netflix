/*
 * Copyright 2013-2015 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.cloud.netflix.metrics.spectator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.junit.Test;

import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Tag;

import static org.junit.Assert.assertEquals;
import static org.springframework.cloud.netflix.metrics.spectator.SpectatorMetricReader.asHierarchicalName;

/**
 * @author Jon Schneider
 */
public class SpectatorMetricReaderTests {
	@Test
	public void convertSpectatorMetricWithTagsToHierarchicalName() {
		Id mWithTags = new SimpleId("m", "t1", "val1", "t2", "val2");
		assertEquals("m(t1=val1,t2=val2)", asHierarchicalName(mWithTags));
	}

	private class SimpleTag implements Tag {
		String key;
		String value;

		public SimpleTag(String key, String value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public String key() {
			return key;
		}

		@Override
		public String value() {
			return value;
		}
	}

	private class SimpleId implements Id {
		String name;
		Collection<Tag> tags;

		public SimpleId(String name, String... tagPairs) {
			this.name = name;
			tags = new ArrayList<>();
			for (int i = 0; i < tagPairs.length; i += 2)
				tags.add(new SimpleTag(tagPairs[i], tagPairs[i + 1]));
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public Iterable<Tag> tags() {
			return tags;
		}

		@Override
		public Id withTag(String s, String s1) {
			return null;
		}

		@Override
		public Id withTag(Tag tag) {
			return null;
		}

		@Override
		public Id withTags(Iterable<Tag> iterable) {
			return null;
		}

		@Override
		public Id withTags(Map<String, String> map) {
			return null;
		}
	}
}
