/*
 * Copyright 2021 the original author or authors.
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

package org.springframework.data.elasticsearch.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.cluster.metadata.AliasMetadata;
import org.elasticsearch.cluster.metadata.MappingMetadata;
import org.elasticsearch.common.compress.CompressedXContent;
import org.elasticsearch.common.settings.Settings;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.AliasData;
import org.springframework.data.elasticsearch.core.mapping.IndexInformation;

/**
 * Factory class to elasticsearch responses to different type of data classes.
 * @author George Popides
 * @since 4.2
 */
public class ResponseConverter {
	public ResponseConverter() {
	}

	// region alias

	public static AliasData convertAliasMetadata(AliasMetadata aliasMetaData) {
		Document filter = null;
		CompressedXContent aliasMetaDataFilter = aliasMetaData.getFilter();
		if (aliasMetaDataFilter != null) {
			filter = Document.parse(aliasMetaDataFilter.string());
		}
		AliasData aliasData = AliasData.of(aliasMetaData.alias(), filter, aliasMetaData.indexRouting(),
				aliasMetaData.getSearchRouting(), aliasMetaData.writeIndex(), aliasMetaData.isHidden());
		return aliasData;
	}

	public List<IndexInformation> indexInformationCollection(GetIndexResponse getIndexResponse) {
		List<IndexInformation> indexInformationList = new ArrayList<>();

		for (String indexName : getIndexResponse.getIndices()) {
			Document settings = settingsFromGetIndexResponse(getIndexResponse, indexName);
			Document mappings = mappingsFromGetIndexResponse(getIndexResponse, indexName);
			List<AliasData> aliases = mappingsFromIndexResponse(getIndexResponse, indexName);


			indexInformationList.add(IndexInformation.create(indexName, settings, mappings, aliases));
		}

		return indexInformationList;
	}

	public List<IndexInformation> indexInformationCollection(org.elasticsearch.action.admin.indices.get.GetIndexResponse getIndexResponse) {
		List<IndexInformation> indexInformationList = new ArrayList<>();

		for (String indexName : getIndexResponse.getIndices()) {
			Document settings = settingsFromGetIndexResponse(getIndexResponse, indexName);
			Document mappings = mappingsFromGetIndexResponse(getIndexResponse, indexName);
			List<AliasData> aliases = mappingsFromIndexResponse(getIndexResponse, indexName);

			indexInformationList.add(IndexInformation.create(indexName, settings, mappings, aliases));
		}

		return indexInformationList;
	}

	public Map<String, Set<AliasData>> convertAliasesResponse(Map<String, Set<AliasMetadata>> aliasesResponse) {
		Map<String, Set<AliasData>> converted = new LinkedHashMap<>();
		aliasesResponse.forEach((index, aliasMetaDataSet) -> {
			Set<AliasData> aliasDataSet = new LinkedHashSet<>();
			aliasMetaDataSet.forEach(aliasMetaData -> aliasDataSet.add(convertAliasMetadata(aliasMetaData)));
			converted.put(index, aliasDataSet);
		});
		return converted;
	}


	// end region


	/**
	 * extract the index settings information from a given index
	 * @param getIndexResponse the elastic GetIndexResponse
	 * @param indexName the index name
	 * @return a document that represents {@link Settings}
	 */
	private Document settingsFromGetIndexResponse(GetIndexResponse getIndexResponse, String indexName) {
		Document document = Document.create();

		Settings indexSettings = getIndexResponse.getSettings().get(indexName);

		if (!indexSettings.isEmpty()) {
			for (String key : indexSettings.keySet()) {
				document.put(key, indexSettings.get(key));
			}
		}

		return document;
	}

	/**
	 * extract the mappings information from a given index
	 * @param getIndexResponse the elastic GetIndexResponse
	 * @param indexName the index name
	 * @return a document that represents {@link MappingMetadata}
	 */
	private Document mappingsFromGetIndexResponse(GetIndexResponse getIndexResponse, String indexName) {
		Document document = Document.create();

		if (getIndexResponse.getMappings().containsKey(indexName)) {
			MappingMetadata mappings = getIndexResponse.getMappings().get(indexName);
			document = Document.from(mappings.getSourceAsMap());
		}

		return document;
	}

	private Document settingsFromGetIndexResponse(org.elasticsearch.action.admin.indices.get.GetIndexResponse getIndexResponse, String indexName) {
		Document document = Document.create();

		if (getIndexResponse.getSettings().containsKey(indexName)) {
			Settings indexSettings = getIndexResponse.getSettings().get(indexName);

			for (String key : indexSettings.keySet()) {
				document.put(key, indexSettings.get(key));
			}
		}

		return document;
	}

	private Document mappingsFromGetIndexResponse(org.elasticsearch.action.admin.indices.get.GetIndexResponse getIndexResponse, String indexName) {
		Document document = Document.create();

		boolean responseHasMappings = getIndexResponse.getMappings().containsKey(indexName) &&
				(getIndexResponse.getMappings().get(indexName).get("_doc") != null);

		if (responseHasMappings) {
			MappingMetadata mappings = getIndexResponse.getMappings().get(indexName).get("_doc");
			document = Document.from(mappings.getSourceAsMap());
		}

		return document;
	}

	private List<AliasData> mappingsFromIndexResponse(GetIndexResponse getIndexResponse, String indexName) {
		List<AliasData> aliases = Collections.emptyList();

		if (getIndexResponse.getAliases().get(indexName) != null) {
			aliases = getIndexResponse
					.getAliases()
					.get(indexName)
					.stream()
					.map(ResponseConverter::convertAliasMetadata)
					.collect(Collectors.toList());
		}
		return aliases;
	}

	private List<AliasData> mappingsFromIndexResponse(org.elasticsearch.action.admin.indices.get.GetIndexResponse getIndexResponse, String indexName) {
		List<AliasData> aliases = Collections.emptyList();

		if (getIndexResponse.getAliases().get(indexName) != null) {
			aliases = getIndexResponse
					.getAliases()
					.get(indexName)
					.stream()
					.map(ResponseConverter::convertAliasMetadata)
					.collect(Collectors.toList());
		}
		return aliases;
	}

}
