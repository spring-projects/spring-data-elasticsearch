/*
 * Copyright 2021-2023 the original author or authors.
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

package org.springframework.data.elasticsearch.client.erhlc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.client.indices.GetIndexTemplatesResponse;
import org.elasticsearch.client.indices.IndexTemplateMetadata;
import org.elasticsearch.cluster.metadata.AliasMetadata;
import org.elasticsearch.cluster.metadata.MappingMetadata;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.compress.CompressedXContent;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.ScrollableHitSource;
import org.springframework.data.elasticsearch.core.IndexInformation;
import org.springframework.data.elasticsearch.core.MultiGetItem;
import org.springframework.data.elasticsearch.core.cluster.ClusterHealth;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.AliasData;
import org.springframework.data.elasticsearch.core.index.Settings;
import org.springframework.data.elasticsearch.core.index.TemplateData;
import org.springframework.data.elasticsearch.core.query.ByQueryResponse;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.elasticsearch.core.reindex.ReindexResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Factory class to convert elasticsearch responses to different type of data classes.
 *
 * @author George Popides
 * @author Peter-Josef Meisch
 * @author Sijia Liu
 * @since 4.2
 * @deprecated since 5.0
 */
@Deprecated
public class ResponseConverter {
	private ResponseConverter() {}

	// region alias

	public static Map<String, Set<AliasData>> aliasDatas(Map<String, Set<AliasMetadata>> aliasesMetadatas) {
		Map<String, Set<AliasData>> converted = new LinkedHashMap<>();
		aliasesMetadatas.forEach((index, aliasMetaDataSet) -> {
			Set<AliasData> aliasDataSet = new LinkedHashSet<>();
			aliasMetaDataSet.forEach(aliasMetaData -> aliasDataSet.add(toAliasData(aliasMetaData)));
			converted.put(index, aliasDataSet);
		});
		return converted;
	}

	public static AliasData toAliasData(AliasMetadata aliasMetaData) {

		CompressedXContent aliasMetaDataFilter = aliasMetaData.getFilter();
		Query filterQuery = (aliasMetaDataFilter != null) ? StringQuery.builder(aliasMetaDataFilter.string()).build()
				: null;
		return AliasData.of(aliasMetaData.alias(), filterQuery, aliasMetaData.indexRouting(),
				aliasMetaData.getSearchRouting(), aliasMetaData.writeIndex(), aliasMetaData.isHidden());
	}
	// endregion

	// region index informations
	/**
	 * get the index informations from a {@link GetIndexResponse}
	 *
	 * @param getIndexResponse the index response, must not be {@literal null}
	 * @return list of {@link IndexInformation}s for the different indices
	 */
	public static List<IndexInformation> getIndexInformations(GetIndexResponse getIndexResponse) {

		Assert.notNull(getIndexResponse, "getIndexResponse must not be null");

		List<IndexInformation> indexInformationList = new ArrayList<>();

		for (String indexName : getIndexResponse.getIndices()) {
			Settings settings = settingsFromGetIndexResponse(getIndexResponse, indexName);
			Document mappings = mappingsFromGetIndexResponse(getIndexResponse, indexName);
			List<AliasData> aliases = aliasDataFromIndexResponse(getIndexResponse, indexName);

			indexInformationList.add(IndexInformation.of(indexName, settings, mappings, aliases));
		}

		return indexInformationList;
	}

	/**
	 * extract the index settings information from a given index
	 *
	 * @param getIndexResponse the elastic GetIndexResponse
	 * @param indexName the index name
	 * @return a document that represents {@link Settings}
	 */
	private static Settings settingsFromGetIndexResponse(GetIndexResponse getIndexResponse, String indexName) {
		Settings settings = new Settings();

		org.elasticsearch.common.settings.Settings indexSettings = getIndexResponse.getSettings().get(indexName);

		if (!indexSettings.isEmpty()) {
			for (String key : indexSettings.keySet()) {
				settings.put(key, indexSettings.get(key));
			}
		}

		return settings;
	}

	/**
	 * extract the mappings information from a given index
	 *
	 * @param getIndexResponse the elastic GetIndexResponse
	 * @param indexName the index name
	 * @return a document that represents {@link MappingMetadata}
	 */
	private static Document mappingsFromGetIndexResponse(GetIndexResponse getIndexResponse, String indexName) {
		Document document = Document.create();

		if (getIndexResponse.getMappings().containsKey(indexName)) {
			MappingMetadata mappings = getIndexResponse.getMappings().get(indexName);
			document = Document.from(mappings.getSourceAsMap());
		}

		return document;
	}

	private static List<AliasData> aliasDataFromIndexResponse(GetIndexResponse getIndexResponse, String indexName) {
		List<AliasData> aliases = Collections.emptyList();

		if (getIndexResponse.getAliases().get(indexName) != null) {
			aliases = getIndexResponse.getAliases().get(indexName).stream().map(ResponseConverter::toAliasData)
					.collect(Collectors.toList());
		}
		return aliases;
	}

	/**
	 * get the index informations from a {@link org.elasticsearch.action.admin.indices.get.GetIndexResponse} (transport
	 * client)
	 *
	 * @param getIndexResponse the index response, must not be {@literal null}
	 * @return list of {@link IndexInformation}s for the different indices
	 */
	public static List<IndexInformation> getIndexInformations(
			org.elasticsearch.action.admin.indices.get.GetIndexResponse getIndexResponse) {
		List<IndexInformation> indexInformationList = new ArrayList<>();

		for (String indexName : getIndexResponse.getIndices()) {
			Settings settings = settingsFromGetIndexResponse(getIndexResponse, indexName);
			Document mappings = mappingsFromGetIndexResponse(getIndexResponse, indexName);
			List<AliasData> aliases = aliasDataFromIndexResponse(getIndexResponse, indexName);

			indexInformationList.add(IndexInformation.of(indexName, settings, mappings, aliases));
		}

		return indexInformationList;
	}

	private static Settings settingsFromGetIndexResponse(
			org.elasticsearch.action.admin.indices.get.GetIndexResponse getIndexResponse, String indexName) {

		Settings settings = new Settings();

		if (getIndexResponse.getSettings().containsKey(indexName)) {
			org.elasticsearch.common.settings.Settings indexSettings = getIndexResponse.getSettings().get(indexName);

			for (String key : indexSettings.keySet()) {
				settings.put(key, indexSettings.get(key));
			}
		}

		return settings;
	}

	private static Document mappingsFromGetIndexResponse(
			org.elasticsearch.action.admin.indices.get.GetIndexResponse getIndexResponse, String indexName) {
		Document document = Document.create();

		boolean responseHasMappings = getIndexResponse.getMappings().containsKey(indexName)
				&& (getIndexResponse.getMappings().get(indexName).get("_doc") != null);

		if (responseHasMappings) {
			MappingMetadata mappings = getIndexResponse.getMappings().get(indexName).get("_doc");
			document = Document.from(mappings.getSourceAsMap());
		}

		return document;
	}

	private static List<AliasData> aliasDataFromIndexResponse(
			org.elasticsearch.action.admin.indices.get.GetIndexResponse getIndexResponse, String indexName) {
		List<AliasData> aliases = Collections.emptyList();

		if (getIndexResponse.getAliases().get(indexName) != null) {
			aliases = getIndexResponse.getAliases().get(indexName).stream().map(ResponseConverter::toAliasData)
					.collect(Collectors.toList());
		}
		return aliases;
	}

	// endregion

	// region templates
	@Nullable
	public static TemplateData getTemplateData(GetIndexTemplatesResponse getIndexTemplatesResponse, String templateName) {
		for (IndexTemplateMetadata indexTemplateMetadata : getIndexTemplatesResponse.getIndexTemplates()) {

			if (indexTemplateMetadata.name().equals(templateName)) {

				Settings settings = new Settings();
				org.elasticsearch.common.settings.Settings templateSettings = indexTemplateMetadata.settings();
				templateSettings.keySet().forEach(key -> settings.put(key, templateSettings.get(key)));

				Map<String, AliasData> aliases = new LinkedHashMap<>();

				ImmutableOpenMap<String, AliasMetadata> aliasesResponse = indexTemplateMetadata.aliases();
				Iterator<String> keysIt = aliasesResponse.keysIt();
				while (keysIt.hasNext()) {
					String key = keysIt.next();
					aliases.put(key, ResponseConverter.toAliasData(aliasesResponse.get(key)));
				}

				return TemplateData.builder().withIndexPatterns(indexTemplateMetadata.patterns().toArray(new String[0])) //
						.withSettings(settings) //
						.withMapping(Document.from(indexTemplateMetadata.mappings().getSourceAsMap())) //
						.withAliases(aliases) //
						.withOrder(indexTemplateMetadata.order()) //
						.withVersion(indexTemplateMetadata.version()).build();
			}
		}
		return null;
	}
	// endregion

	// region settings
	/**
	 * extract the index settings information for a given index
	 *
	 * @param response the Elasticsearch response
	 * @param indexName the index name
	 * @return settings
	 */
	public static Settings fromSettingsResponse(GetSettingsResponse response, String indexName) {

		Settings settings = new Settings();

		if (!response.getIndexToDefaultSettings().isEmpty()) {
			org.elasticsearch.common.settings.Settings defaultSettings = response.getIndexToDefaultSettings().get(indexName);
			for (String key : defaultSettings.keySet()) {
				settings.put(key, defaultSettings.get(key));
			}
		}

		if (!response.getIndexToSettings().isEmpty()) {
			org.elasticsearch.common.settings.Settings customSettings = response.getIndexToSettings().get(indexName);
			for (String key : customSettings.keySet()) {
				settings.put(key, customSettings.get(key));
			}
		}

		return settings;
	}
	// endregion

	// region multiget

	@Nullable
	public static MultiGetItem.Failure getFailure(MultiGetItemResponse itemResponse) {

		MultiGetResponse.Failure responseFailure = itemResponse.getFailure();
		return responseFailure != null ? MultiGetItem.Failure.of(responseFailure.getIndex(), responseFailure.getType(),
				responseFailure.getId(), responseFailure.getFailure(), null) : null;
	}
	// endregion

	// region cluster operations
	public static ClusterHealth clusterHealth(ClusterHealthResponse clusterHealthResponse) {
		return ClusterHealth.builder() //
				.withActivePrimaryShards(clusterHealthResponse.getActivePrimaryShards()) //
				.withActiveShards(clusterHealthResponse.getActiveShards()) //
				.withActiveShardsPercent(clusterHealthResponse.getActiveShardsPercent()) //
				.withClusterName(clusterHealthResponse.getClusterName()) //
				.withDelayedUnassignedShards(clusterHealthResponse.getDelayedUnassignedShards()) //
				.withInitializingShards(clusterHealthResponse.getInitializingShards()) //
				.withNumberOfDataNodes(clusterHealthResponse.getNumberOfDataNodes()) //
				.withNumberOfInFlightFetch(clusterHealthResponse.getNumberOfInFlightFetch()) //
				.withNumberOfNodes(clusterHealthResponse.getNumberOfNodes()) //
				.withNumberOfPendingTasks(clusterHealthResponse.getNumberOfPendingTasks()) //
				.withRelocatingShards(clusterHealthResponse.getRelocatingShards()) //
				.withStatus(clusterHealthResponse.getStatus().toString()) //
				.withTaskMaxWaitingTimeMillis(clusterHealthResponse.getTaskMaxWaitingTime().millis()) //
				.withTimedOut(clusterHealthResponse.isTimedOut()) //
				.withUnassignedShards(clusterHealthResponse.getUnassignedShards()) //
				.build(); //

	}
	// endregion

	// region byQueryResponse
	public static ByQueryResponse byQueryResponseOf(BulkByScrollResponse bulkByScrollResponse) {
		final List<ByQueryResponse.Failure> failures = bulkByScrollResponse.getBulkFailures() //
				.stream() //
				.map(ResponseConverter::byQueryResponseFailureOf) //
				.collect(Collectors.toList()); //

		final List<ByQueryResponse.SearchFailure> searchFailures = bulkByScrollResponse.getSearchFailures() //
				.stream() //
				.map(ResponseConverter::byQueryResponseSearchFailureOf) //
				.collect(Collectors.toList());//

		return ByQueryResponse.builder() //
				.withTook(bulkByScrollResponse.getTook().getMillis()) //
				.withTimedOut(bulkByScrollResponse.isTimedOut()) //
				.withTotal(bulkByScrollResponse.getTotal()) //
				.withUpdated(bulkByScrollResponse.getUpdated()) //
				.withDeleted(bulkByScrollResponse.getDeleted()) //
				.withBatches(bulkByScrollResponse.getBatches()) //
				.withVersionConflicts(bulkByScrollResponse.getVersionConflicts()) //
				.withNoops(bulkByScrollResponse.getNoops()) //
				.withBulkRetries(bulkByScrollResponse.getBulkRetries()) //
				.withSearchRetries(bulkByScrollResponse.getSearchRetries()) //
				.withReasonCancelled(bulkByScrollResponse.getReasonCancelled()) //
				.withFailures(failures) //
				.withSearchFailure(searchFailures) //
				.build(); //
	}

	/**
	 * Create a new {@link ByQueryResponse.Failure} from {@link BulkItemResponse.Failure}
	 *
	 * @param failure {@link BulkItemResponse.Failure} to translate
	 * @return a new {@link ByQueryResponse.Failure}
	 */
	public static ByQueryResponse.Failure byQueryResponseFailureOf(BulkItemResponse.Failure failure) {
		return ByQueryResponse.Failure.builder() //
				.withIndex(failure.getIndex()) //
				.withType(failure.getType()) //
				.withId(failure.getId()) //
				.withStatus(failure.getStatus().getStatus()) //
				.withAborted(failure.isAborted()) //
				.withCause(failure.getCause()) //
				.withSeqNo(failure.getSeqNo()) //
				.withTerm(failure.getTerm()) //
				.build(); //
	}

	/**
	 * Create a new {@link ByQueryResponse.SearchFailure} from {@link ScrollableHitSource.SearchFailure}
	 *
	 * @param searchFailure {@link ScrollableHitSource.SearchFailure} to translate
	 * @return a new {@link ByQueryResponse.SearchFailure}
	 */
	public static ByQueryResponse.SearchFailure byQueryResponseSearchFailureOf(
			ScrollableHitSource.SearchFailure searchFailure) {
		return ByQueryResponse.SearchFailure.builder() //
				.withReason(searchFailure.getReason()) //
				.withIndex(searchFailure.getIndex()) //
				.withNodeId(searchFailure.getNodeId()) //
				.withShardId(searchFailure.getShardId()) //
				.withStatus(searchFailure.getStatus().getStatus()) //
				.build(); //
	}

	// endregion

	// region reindex
	/**
	 * @since 4.4
	 */
	public static ReindexResponse reindexResponseOf(BulkByScrollResponse bulkByScrollResponse) {
		final List<ReindexResponse.Failure> failures = bulkByScrollResponse.getBulkFailures() //
				.stream() //
				.map(ResponseConverter::reindexResponseFailureOf) //
				.collect(Collectors.toList()); //

		return ReindexResponse.builder() //
				.withTook(bulkByScrollResponse.getTook().getMillis()) //
				.withTimedOut(bulkByScrollResponse.isTimedOut()) //
				.withTotal(bulkByScrollResponse.getTotal()) //
				.withCreated(bulkByScrollResponse.getCreated()) //
				.withUpdated(bulkByScrollResponse.getUpdated()) //
				.withDeleted(bulkByScrollResponse.getDeleted()) //
				.withBatches(bulkByScrollResponse.getBatches()) //
				.withVersionConflicts(bulkByScrollResponse.getVersionConflicts()) //
				.withNoops(bulkByScrollResponse.getNoops()) //
				.withBulkRetries(bulkByScrollResponse.getBulkRetries()) //
				.withSearchRetries(bulkByScrollResponse.getSearchRetries()) //
				.withThrottledMillis(bulkByScrollResponse.getStatus().getThrottled().getMillis()) //
				.withRequestsPerSecond(bulkByScrollResponse.getStatus().getRequestsPerSecond()) //
				.withThrottledUntilMillis(bulkByScrollResponse.getStatus().getThrottledUntil().getMillis()) //
				.withFailures(failures) //
				.build(); //

	}

	/**
	 * @since 4.4
	 */
	public static ReindexResponse.Failure reindexResponseFailureOf(BulkItemResponse.Failure failure) {
		return ReindexResponse.Failure.builder() //
				.withIndex(failure.getIndex()) //
				.withType(failure.getType()) //
				.withId(failure.getId()) //
				.withStatus(failure.getStatus().getStatus()) //
				.withAborted(failure.isAborted()) //
				.withCause(failure.getCause()) //
				.withSeqNo(failure.getSeqNo()) //
				.withTerm(failure.getTerm()) //
				.build(); //
	}

	// endregion
}
