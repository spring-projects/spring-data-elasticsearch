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
import java.util.List;

import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.cluster.metadata.AliasMetadata;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexInformation;

/**
 * Factory class to elasticsearch responses to different type of data classes.
 * @author George Popides
 * @since 4.2
 */
public class ResponseConverter {
	private final RequestFactory requestFactory;

	public ResponseConverter(RequestFactory requestFactory) {
		this.requestFactory = requestFactory;
	}

	public List<IndexInformation> indexInformationCollection(GetIndexResponse getIndexResponse) {
		List<IndexInformation> indexInformationList = new ArrayList<>();


		for (String indexName : getIndexResponse.getIndices()) {
			Document settings = requestFactory.settingsFromGetIndexResponse(getIndexResponse, indexName);
			Document mappings = requestFactory.mappingsFromGetIndexResponse(getIndexResponse, indexName);
			List<AliasMetadata> aliases = getIndexResponse.getAliases().get(indexName);
			indexInformationList.add(IndexInformation.create(indexName, settings, mappings, aliases));
		}

		return indexInformationList;
	}

	public List<IndexInformation> indexInformationCollection(org.elasticsearch.action.admin.indices.get.GetIndexResponse getIndexResponse) {
		List<IndexInformation> indexInformationList = new ArrayList<>();

		for (String indexName : getIndexResponse.getIndices()) {
			Document settings = requestFactory.settingsFromGetIndexResponse(getIndexResponse, indexName);
			Document mappings = requestFactory.mappingsFromGetIndexResponse(getIndexResponse, indexName);
			List<AliasMetadata> aliases = getIndexResponse.getAliases().get(indexName);
			indexInformationList.add(IndexInformation.create(indexName, settings, mappings, aliases));
		}

		return indexInformationList;
	}

}
