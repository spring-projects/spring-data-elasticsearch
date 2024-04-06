/*
 * Copyright 2021-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.client.elc;

import static org.springframework.data.elasticsearch.client.elc.JsonUtils.*;
import static org.springframework.data.elasticsearch.client.elc.TypeUtils.*;

import co.elastic.clients.elasticsearch._types.BulkIndexByScrollFailure;
import co.elastic.clients.elasticsearch._types.ErrorCause;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.cluster.ComponentTemplateSummary;
import co.elastic.clients.elasticsearch.cluster.GetComponentTemplateResponse;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import co.elastic.clients.elasticsearch.core.DeleteByQueryResponse;
import co.elastic.clients.elasticsearch.core.GetScriptResponse;
import co.elastic.clients.elasticsearch.core.UpdateByQueryResponse;
import co.elastic.clients.elasticsearch.core.mget.MultiGetError;
import co.elastic.clients.elasticsearch.core.mget.MultiGetResponseItem;
import co.elastic.clients.elasticsearch.indices.*;
import co.elastic.clients.elasticsearch.indices.get_index_template.IndexTemplateItem;
import co.elastic.clients.elasticsearch.indices.get_mapping.IndexMappingRecord;
import co.elastic.clients.json.JsonpMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.data.elasticsearch.ElasticsearchErrorCause;
import org.springframework.data.elasticsearch.core.IndexInformation;
import org.springframework.data.elasticsearch.core.MultiGetItem;
import org.springframework.data.elasticsearch.core.cluster.ClusterHealth;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.AliasData;
import org.springframework.data.elasticsearch.core.index.Settings;
import org.springframework.data.elasticsearch.core.index.TemplateData;
import org.springframework.data.elasticsearch.core.index.TemplateResponse;
import org.springframework.data.elasticsearch.core.index.TemplateResponseData;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.ByQueryResponse;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.elasticsearch.core.reindex.ReindexResponse;
import org.springframework.data.elasticsearch.core.script.Script;
import org.springframework.data.elasticsearch.support.DefaultStringObjectMap;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Class to convert Elasticsearch responses into Spring Data Elasticsearch classes.
 *
 * @author Peter-Josef Meisch
 * @since 4.4
 */
class ResponseConverter {

	private static final Log LOGGER = LogFactory.getLog(ResponseConverter.class);

	private final JsonpMapper jsonpMapper;

	public ResponseConverter(JsonpMapper jsonpMapper) {
		this.jsonpMapper = jsonpMapper;
	}

	// region cluster client
	public ClusterHealth clusterHealth(HealthResponse healthResponse) {

		Assert.notNull(healthResponse, "healthResponse must not be null");

		return ClusterHealth.builder() //
				.withActivePrimaryShards(healthResponse.activePrimaryShards()) //
				.withActiveShards(healthResponse.activeShards()) //
				.withActiveShardsPercent(Double.parseDouble(healthResponse.activeShardsPercentAsNumber()))//
				.withClusterName(healthResponse.clusterName()) //
				.withDelayedUnassignedShards(healthResponse.delayedUnassignedShards()) //
				.withInitializingShards(healthResponse.initializingShards()) //
				.withNumberOfDataNodes(healthResponse.numberOfDataNodes()) //
				.withNumberOfInFlightFetch(healthResponse.numberOfInFlightFetch()) //
				.withNumberOfNodes(healthResponse.numberOfNodes()) //
				.withNumberOfPendingTasks(healthResponse.numberOfPendingTasks()) //
				.withRelocatingShards(healthResponse.relocatingShards()) //
				.withStatus(healthResponse.status().toString()) //
				.withTaskMaxWaitingTimeMillis(healthResponse.taskMaxWaitingInQueueMillis()) //
				.withTimedOut(healthResponse.timedOut()) //
				.withUnassignedShards(healthResponse.unassignedShards()) //
				.build(); //
	}

	public List<TemplateResponse> clusterGetComponentTemplates(
			GetComponentTemplateResponse getComponentTemplateResponse) {

		Assert.notNull(getComponentTemplateResponse, "getComponentTemplateResponse must not be null");

		var componentTemplates = new ArrayList<TemplateResponse>();
		getComponentTemplateResponse.componentTemplates().forEach(componentTemplate -> {
			componentTemplates.add(clusterGetComponentTemplate(componentTemplate));
		});

		return componentTemplates;
	}

	private TemplateResponse clusterGetComponentTemplate(
			co.elastic.clients.elasticsearch.cluster.ComponentTemplate componentTemplate) {
		var componentTemplateNode = componentTemplate.componentTemplate();
		var componentTemplateSummary = componentTemplateNode.template();
		return TemplateResponse.builder() //
				.withName(componentTemplate.name()) //
				.withVersion(componentTemplateNode.version()) //
				.withTemplateData(clusterGetComponentTemplateData(componentTemplateSummary)) //
				.build();
	}

	private TemplateResponseData clusterGetComponentTemplateData(ComponentTemplateSummary componentTemplateSummary) {

		var mapping = typeMapping(componentTemplateSummary.mappings());
		var settings = new Settings();
		componentTemplateSummary.settings().forEach((key, indexSettings) -> {
			settings.put(key, Settings.parse(removePrefixFromJson(indexSettings.toString())));
		});

		Function<? super Map.Entry<String, AliasDefinition>, String> keyMapper = Map.Entry::getKey;
		Function<? super Map.Entry<String, AliasDefinition>, AliasData> valueMapper = entry -> indicesGetAliasData(
				entry.getKey(), entry.getValue());

		Map<String, AliasData> aliases = componentTemplateSummary.aliases().entrySet().stream()
				.collect(Collectors.toMap(keyMapper, valueMapper));

		return TemplateResponseData.builder() //
				.withMapping(mapping) //
				.withSettings(settings) //
				.withAliases(aliases) //
				.build();
	}

	// endregion

	// region indices client
	public Settings indicesGetSettings(GetIndicesSettingsResponse getIndicesSettingsResponse, String indexName) {

		Assert.notNull(getIndicesSettingsResponse, "getIndicesSettingsResponse must not be null");
		Assert.notNull(indexName, "indexName must not be null");

		Settings settings = new Settings();
		IndexState indexState = getIndicesSettingsResponse.get(indexName);

		if (indexState != null) {

			Function<IndexSettings, Settings> indexSettingsToSettings = indexSettings -> {
				Settings parsedSettings = Settings.parse(toJson(indexSettings, jsonpMapper));
				return (indexSettings.index() != null) ? parsedSettings : new Settings().append("index", parsedSettings);
			};

			if (indexState.defaults() != null) {
				Settings defaultSettings = indexSettingsToSettings.apply(indexState.defaults());
				settings.merge(defaultSettings);
			}

			if (indexState.settings() != null) {
				Settings nonDefaultSettings = indexSettingsToSettings.apply(indexState.settings());
				settings.merge(nonDefaultSettings);
			}
		}

		return settings;
	}

	public Document indicesGetMapping(GetMappingResponse getMappingResponse, IndexCoordinates indexCoordinates) {

		Assert.notNull(getMappingResponse, "getMappingResponse must not be null");
		Assert.notNull(indexCoordinates, "indexCoordinates must not be null");

		Map<String, IndexMappingRecord> mappings = getMappingResponse.result();

		if (mappings == null || mappings.isEmpty()) {
			return Document.create();
		}

		IndexMappingRecord indexMappingRecord = mappings.get(indexCoordinates.getIndexName());

		// this can happen when the mapping was requested with an alias
		if (indexMappingRecord == null) {

			if (mappings.size() != 1) {
				LOGGER.warn(String.format("no mapping returned for index %s", indexCoordinates.getIndexName()));
				return Document.create();
			}
			String index = mappings.keySet().iterator().next();
			indexMappingRecord = mappings.get(index);
		}

		return Document.parse(toJson(indexMappingRecord.mappings(), jsonpMapper));
	}

	public List<IndexInformation> indicesGetIndexInformations(GetIndexResponse getIndexResponse) {

		Assert.notNull(getIndexResponse, "getIndexResponse must not be null");

		List<IndexInformation> indexInformationList = new ArrayList<>();

		getIndexResponse.result().forEach((indexName, indexState) -> {
			Settings settings = indexState.settings() != null ? Settings.parse(toJson(indexState.settings(), jsonpMapper))
					: new Settings();
			Document mappings = indexState.mappings() != null ? Document.parse(toJson(indexState.mappings(), jsonpMapper))
					: Document.create();

			List<AliasData> aliasDataList = new ArrayList<>();
			indexState.aliases().forEach((aliasName, alias) -> aliasDataList.add(indicesGetAliasData(aliasName, alias)));

			indexInformationList.add(IndexInformation.of(indexName, settings, mappings, aliasDataList));

		});
		return indexInformationList;
	}

	public Map<String, Set<AliasData>> indicesGetAliasData(GetAliasResponse getAliasResponse) {

		Assert.notNull(getAliasResponse, "getAliasResponse must not be null");

		Map<String, Set<AliasData>> aliasDataMap = new HashMap<>();
		getAliasResponse.result().forEach((indexName, alias) -> {
			Set<AliasData> aliasDataSet = new HashSet<>();
			alias.aliases()
					.forEach((aliasName, aliasDefinition) -> aliasDataSet.add(indicesGetAliasData(aliasName, aliasDefinition)));
			aliasDataMap.put(indexName, aliasDataSet);
		});
		return aliasDataMap;
	}

	private AliasData indicesGetAliasData(String aliasName, Alias alias) {

		Query filter = alias.filter();
		String filterJson = filter != null ? toJson(filter, jsonpMapper) : null;
		var filterQuery = filterJson != null ? StringQuery.builder(filterJson).build() : null;
		return AliasData.of(aliasName, filterQuery, alias.indexRouting(), alias.searchRouting(), alias.isWriteIndex(),
				alias.isHidden());
	}

	private AliasData indicesGetAliasData(String aliasName, AliasDefinition alias) {
		Query filter = alias.filter();
		String filterJson = filter != null ? toJson(filter, jsonpMapper) : null;
		var filterQuery = filterJson != null ? StringQuery.builder(filterJson).build() : null;
		return AliasData.of(aliasName, filterQuery, alias.indexRouting(), alias.searchRouting(), alias.isWriteIndex(),
				null);
	}

	@Nullable
	public TemplateData indicesGetTemplateData(GetTemplateResponse getTemplateResponse, String templateName) {

		Assert.notNull(getTemplateResponse, "getTemplateResponse must not be null");
		Assert.notNull(templateName, "templateName must not be null");

		TemplateMapping templateMapping = getTemplateResponse.get(templateName);
		if (templateMapping != null) {

			Settings settings = new Settings();
			templateMapping.settings().forEach((key, jsonData) -> {

				if (key.contains(".")) {
					// returned string contains " quotes
					settings.put(key, jsonData.toJson().toString().replaceAll("^\"|\"$", ""));
				} else {
					settings.put(key, new DefaultStringObjectMap<>().fromJson(jsonData.toJson().toString()));
				}
			});

			Function<? super Map.Entry<String, Alias>, String> keyMapper = Map.Entry::getKey;
			Function<? super Map.Entry<String, Alias>, AliasData> valueMapper = entry -> indicesGetAliasData(entry.getKey(),
					entry.getValue());

			Map<String, AliasData> aliases = templateMapping.aliases().entrySet().stream()
					.collect(Collectors.toMap(keyMapper, valueMapper));

			Document mapping = Document.parse(toJson(templateMapping.mappings(), jsonpMapper));

			TemplateData.TemplateDataBuilder builder = TemplateData.builder() //
					.withIndexPatterns(templateMapping.indexPatterns().toArray(new String[0])) //
					.withOrder(templateMapping.order()) //
					.withSettings(settings) //
					.withMapping(mapping) //
					.withAliases(aliases) //
			;

			if (templateMapping.version() != null) {
				builder.withVersion(templateMapping.version().intValue());
			}

			return builder.build();
		}

		return null;
	}

	public List<TemplateResponse> getIndexTemplates(GetIndexTemplateResponse getIndexTemplateResponse) {

		Assert.notNull(getIndexTemplateResponse, "getIndexTemplateResponse must not be null");

		var componentTemplates = new ArrayList<TemplateResponse>();
		getIndexTemplateResponse.indexTemplates().forEach(indexTemplateItem -> {
			componentTemplates.add(indexGetComponentTemplate(indexTemplateItem));
		});

		return componentTemplates;
	}

	private TemplateResponse indexGetComponentTemplate(IndexTemplateItem indexTemplateItem) {
		var indexTemplate = indexTemplateItem.indexTemplate();
		var composedOf = indexTemplate.composedOf();
		var indexTemplateSummary = indexTemplate.template();
		return TemplateResponse.builder() //
				.withName(indexTemplateItem.name()) //
				.withVersion(indexTemplate.version()) //
				.withTemplateData(indexGetComponentTemplateData(indexTemplateSummary, composedOf)) //
				.build();
	}

	private TemplateResponseData indexGetComponentTemplateData(IndexTemplateSummary indexTemplateSummary,
			List<String> composedOf) {
		var mapping = typeMapping(indexTemplateSummary.mappings());

		Function<IndexSettings, Settings> indexSettingsToSettings = indexSettings -> {

			if (indexSettings == null) {
				return null;
			}

			Settings parsedSettings = Settings.parse(toJson(indexSettings, jsonpMapper));
			return (indexSettings.index() != null) ? parsedSettings : new Settings().append("index", parsedSettings);
		};
		var settings = indexSettingsToSettings.apply(indexTemplateSummary.settings());

		Function<? super Map.Entry<String, Alias>, String> keyMapper = Map.Entry::getKey;
		Function<? super Map.Entry<String, Alias>, AliasData> valueMapper = entry -> indicesGetAliasData(entry.getKey(),
				entry.getValue());

		Map<String, Alias> aliases1 = indexTemplateSummary.aliases();
		Map<String, AliasData> aliases = aliases1.entrySet().stream().collect(Collectors.toMap(keyMapper, valueMapper));

		return TemplateResponseData.builder() //
				.withMapping(mapping) //
				.withSettings(settings) //
				.withAliases(aliases) //
				.withComposedOf(composedOf) //
				.build();
	}

	// endregion

	// region document operations
	public ReindexResponse reindexResponse(co.elastic.clients.elasticsearch.core.ReindexResponse reindexResponse) {

		Assert.notNull(reindexResponse, "reindexResponse must not be null");

		List<ReindexResponse.Failure> failures = reindexResponse.failures() //
				.stream() //
				.map(this::reindexResponseFailureOf) //
				.collect(Collectors.toList());

		// noinspection ConstantConditions
		return ReindexResponse.builder() //
				.withTook(reindexResponse.took()) //
				.withTimedOut(reindexResponse.timedOut()) //
				.withTotal(reindexResponse.total()) //
				.withCreated(reindexResponse.created()) //
				.withUpdated(reindexResponse.updated()) //
				.withDeleted(reindexResponse.deleted()) //
				.withBatches(reindexResponse.batches()) //
				.withVersionConflicts(reindexResponse.versionConflicts()) //
				.withNoops(reindexResponse.noops()) //
				.withBulkRetries(reindexResponse.retries().bulk()) //
				.withSearchRetries(reindexResponse.retries().search()) //
				.withThrottledMillis(reindexResponse.throttledMillis()) //
				.withRequestsPerSecond(reindexResponse.requestsPerSecond()) //
				.withThrottledUntilMillis(reindexResponse.throttledUntilMillis()) //
				.withFailures(failures) //
				.build();
	}

	private ReindexResponse.Failure reindexResponseFailureOf(BulkIndexByScrollFailure failure) {
		return ReindexResponse.Failure.builder() //
				.withIndex(failure.index()) //
				.withType(failure.type()) //
				.withId(failure.id()) //
				.withStatus(failure.status())//
				.withErrorCause(toErrorCause(failure.cause())) //
				// seqno, term, aborted are not available in the new client
				.build();
	}

	private ByQueryResponse.Failure byQueryResponseFailureOf(BulkIndexByScrollFailure failure) {
		return ByQueryResponse.Failure.builder() //
				.withIndex(failure.index()) //
				.withType(failure.type()) //
				.withId(failure.id()) //
				.withStatus(failure.status())//
				.withErrorCause(toErrorCause(failure.cause())).build();
	}

	@Nullable
	public static MultiGetItem.Failure getFailure(MultiGetResponseItem<EntityAsMap> itemResponse) {

		MultiGetError responseFailure = itemResponse.isFailure() ? itemResponse.failure() : null;

		return responseFailure != null
				? MultiGetItem.Failure.of(responseFailure.index(), null, responseFailure.id(), null,
						toErrorCause(responseFailure.error()))
				: null;
	}

	public ByQueryResponse byQueryResponse(DeleteByQueryResponse response) {
		// the code for the methods taking a DeleteByQueryResponse or a UpdateByQueryResponse is duplicated because the
		// Elasticsearch responses do not share a common class
		// noinspection DuplicatedCode
		List<ByQueryResponse.Failure> failures = response.failures().stream().map(this::byQueryResponseFailureOf)
				.collect(Collectors.toList());

		ByQueryResponse.ByQueryResponseBuilder builder = ByQueryResponse.builder();

		if (response.took() != null) {
			builder.withTook(response.took());
		}

		if (response.timedOut() != null) {
			builder.withTimedOut(response.timedOut());
		}

		if (response.total() != null) {
			builder.withTotal(response.total());
		}

		if (response.deleted() != null) {
			builder.withDeleted(response.deleted());
		}

		if (response.batches() != null) {
			builder.withBatches(Math.toIntExact(response.batches()));
		}

		if (response.versionConflicts() != null) {
			builder.withVersionConflicts(response.versionConflicts());
		}

		if (response.noops() != null) {
			builder.withNoops(response.noops());
		}

		if (response.retries() != null) {
			builder.withBulkRetries(response.retries().bulk());
			builder.withSearchRetries(response.retries().search());
		}

		builder.withFailures(failures);

		return builder.build();
	}

	public ByQueryResponse byQueryResponse(UpdateByQueryResponse response) {
		// the code for the methods taking a DeleteByQueryResponse or a UpdateByQueryResponse is duplicated because the
		// Elasticsearch responses do not share a common class
		// noinspection DuplicatedCode
		List<ByQueryResponse.Failure> failures = response.failures().stream().map(this::byQueryResponseFailureOf)
				.collect(Collectors.toList());

		ByQueryResponse.ByQueryResponseBuilder builder = ByQueryResponse.builder();

		if (response.took() != null) {
			builder.withTook(response.took());
		}

		if (response.timedOut() != null) {
			builder.withTimedOut(response.timedOut());
		}

		if (response.total() != null) {
			builder.withTotal(response.total());
		}

		if (response.deleted() != null) {
			builder.withDeleted(response.deleted());
		}

		if (response.batches() != null) {
			builder.withBatches(Math.toIntExact(response.batches()));
		}

		if (response.versionConflicts() != null) {
			builder.withVersionConflicts(response.versionConflicts());
		}

		if (response.noops() != null) {
			builder.withNoops(response.noops());
		}

		if (response.retries() != null) {
			builder.withBulkRetries(response.retries().bulk());
			builder.withSearchRetries(response.retries().search());
		}

		builder.withFailures(failures);

		return builder.build();
	}

	// endregion

	// region script API
	@Nullable
	public Script scriptResponse(GetScriptResponse response) {

		Assert.notNull(response, "response must not be null");

		return response.found() //
				? Script.builder() //
						.withId(response.id()) //
						.withLanguage(response.script().lang()) //
						.withSource(response.script().source()).build() //
				: null;
	}
	// endregion

	// region helper functions

	private long timeToLong(Time time) {

		if (time.isTime()) {
			return Long.parseLong(time.time());
		} else {
			return time.offset();
		}
	}

	@Nullable
	static ElasticsearchErrorCause toErrorCause(@Nullable ErrorCause errorCause) {

		if (errorCause != null) {
			return new ElasticsearchErrorCause( //
					errorCause.type(), //
					errorCause.reason(), //
					errorCause.stackTrace(), //
					toErrorCause(errorCause.causedBy()), //
					errorCause.rootCause().stream().map(ResponseConverter::toErrorCause).collect(Collectors.toList()), //
					errorCause.suppressed().stream().map(ResponseConverter::toErrorCause).collect(Collectors.toList()));
		} else {
			return null;
		}
	}
	// endregion
}
