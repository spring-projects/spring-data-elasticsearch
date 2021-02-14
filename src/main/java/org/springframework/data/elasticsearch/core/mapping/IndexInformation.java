/*
 * Copyright 2018-2020 the original author or authors.
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

package org.springframework.data.elasticsearch.core.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.cluster.metadata.AliasMetadata;
import org.elasticsearch.cluster.metadata.MappingMetadata;
import org.elasticsearch.common.settings.Settings;

/**
 * Immutable object that holds information(name, settings, mappings, aliases) about an Index
 *
 * @author George Popides
 * @since 4.2
 */
public class IndexInformation {
	private final String name;
	private final Settings settings;
	private final MappingMetadata mappings;
	private final List<AliasMetadata> aliases;


	public static List<IndexInformation> createList(GetIndexResponse getIndexResponse) {
		return buildIndexInformationList(getIndexResponse);
	}

	public static List<IndexInformation> createList(org.elasticsearch.action.admin.indices.get.GetIndexResponse getIndexResponse) {
		return buildIndexInformationList(getIndexResponse);
	}

	private static List<IndexInformation> buildIndexInformationList(GetIndexResponse response) {
		List<IndexInformation> indexInformationList = new ArrayList<>();
		for (String indexName : response.getIndices()) {
			indexInformationList.add(IndexInformation.of(indexName, response));
		}
		return indexInformationList;
	}

	private static List<IndexInformation> buildIndexInformationList(org.elasticsearch.action.admin.indices.get.GetIndexResponse response) {
		List<IndexInformation> indexInformationList = new ArrayList<>();
		for (String indexName : response.getIndices()) {
			indexInformationList.add(IndexInformation.of(indexName, response));
		}
		return indexInformationList;
	}

	private static IndexInformation of(String indexName, org.elasticsearch.action.admin.indices.get.GetIndexResponse getIndexResponse) {
		Settings settings = getIndexResponse.getSettings().get(indexName);
		MappingMetadata mappingMetadata = getIndexResponse.getMappings().get(indexName).get("indexName");
		List<AliasMetadata> aliases = getIndexResponse.getAliases().get(indexName);

		return new IndexInformation(indexName, settings, mappingMetadata, aliases);
	}

	private static IndexInformation of(String indexName, GetIndexResponse getIndexResponse) {
		Settings settings = getIndexResponse.getSettings().get(indexName);
		MappingMetadata mappingMetadata = getIndexResponse.getMappings().get(indexName);
		List<AliasMetadata> aliases = getIndexResponse.getAliases().get(indexName);

		return new IndexInformation(indexName, settings, mappingMetadata, aliases);
	}

	public String getName() {
		return name;
	}

	public Settings getSettings() {
		return settings;
	}

	public List<AliasMetadata> getAliases() {
		return aliases;
	}

	public MappingMetadata getMappings() {
		return mappings;
	}

	private IndexInformation(String name, Settings settings, MappingMetadata mappingMetadata, List<AliasMetadata> aliases) {
		this.name = name;
		this.settings = settings;
		this.mappings = mappingMetadata;
		this.aliases = aliases;
	}

	@Override
	public String toString() {
		return "IndexInformation{" + "indexName=" + name + "}";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (o != null && this.getClass() == o.getClass()) {
			IndexInformation that = (IndexInformation)o;
			return Objects.equals(this.name, that.name);
		} else {
			return false;
		}
	}
}
