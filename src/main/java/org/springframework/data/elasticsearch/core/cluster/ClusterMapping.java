/*
 * Copyright 2024 the original author or authors.
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
package org.springframework.data.elasticsearch.core.cluster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

/**
 * Retrieves mapping definitions of all indices in the cluster.
 *
 * @author Youssef Aouichaoui
 * @since 5.4
 */
public class ClusterMapping implements Iterable<ClusterMapping.ClusterMappingEntry> {
	private final List<ClusterMappingEntry> mappings;

	private ClusterMapping(Builder builder) {
		this.mappings = builder.mappings;
	}

	@NotNull
	@Override
	public Iterator<ClusterMappingEntry> iterator() {
		return mappings.iterator();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class ClusterMappingEntry {
		private final String name;
		private final Map<String, Object> mappings;

		private ClusterMappingEntry(Builder builder) {
			this.name = builder.name;
			this.mappings = builder.mappings;
		}

		public String getName() {
			return name;
		}

		public Map<String, Object> getMappings() {
			return Collections.unmodifiableMap(mappings);
		}

		public static Builder builder(String name) {
			return new Builder(name);
		}

		public static class Builder {
			private final String name;
			private final Map<String, Object> mappings = new HashMap<>();

			private Builder(String name) {
				this.name = name;
			}

			public Builder withMappings(Map<String, Object> mappings) {
				this.mappings.putAll(mappings);

				return this;
			}

			public ClusterMappingEntry build() {
				return new ClusterMappingEntry(this);
			}
		}
	}

	public static class Builder {
		private final List<ClusterMappingEntry> mappings = new ArrayList<>();

		private Builder() {}

		public Builder withMapping(ClusterMappingEntry entry) {
			mappings.add(entry);

			return this;
		}

		public ClusterMapping build() {
			return new ClusterMapping(this);
		}
	}
}
