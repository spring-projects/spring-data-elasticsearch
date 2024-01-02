/*
 * Copyright 2022-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.core.reindex;

import java.time.Duration;

import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.SourceFilter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Request to reindex some documents from one index to another. (@see
 * https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-reindex.html)
 *
 * @author Sijia Liu
 * @since 4.4
 */
public class ReindexRequest {

	// Request body
	private final Source source;
	private final Dest dest;
	@Nullable private final Long maxDocs;
	@Nullable private final Conflicts conflicts;
	@Nullable private final Script script;

	// Query parameters
	@Nullable private final Duration timeout;
	@Nullable private final Boolean requireAlias;
	@Nullable private final Boolean refresh;
	@Nullable private final String waitForActiveShards;
	@Nullable private final Long requestsPerSecond;
	@Nullable private final Duration scroll;
	@Nullable private final Long slices;

	private ReindexRequest(Source source, Dest dest, @Nullable Long maxDocs, @Nullable Conflicts conflicts,
			@Nullable Script script, @Nullable Duration timeout, @Nullable Boolean requireAlias, @Nullable Boolean refresh,
			@Nullable String waitForActiveShards, @Nullable Long requestsPerSecond, @Nullable Duration scroll,
			@Nullable Long slices) {

		Assert.notNull(source, "source must not be null");
		Assert.notNull(dest, "dest must not be null");

		this.source = source;
		this.dest = dest;
		this.maxDocs = maxDocs;
		this.conflicts = conflicts;
		this.script = script;
		this.timeout = timeout;
		this.requireAlias = requireAlias;
		this.refresh = refresh;
		this.waitForActiveShards = waitForActiveShards;
		this.requestsPerSecond = requestsPerSecond;
		this.scroll = scroll;
		this.slices = slices;
	}

	@Nullable
	public Long getMaxDocs() {
		return maxDocs;
	}

	public Source getSource() {
		return source;
	}

	public Dest getDest() {
		return dest;
	}

	@Nullable
	public Script getScript() {
		return script;
	}

	@Nullable
	public Conflicts getConflicts() {
		return conflicts;
	}

	@Nullable
	public Boolean getRequireAlias() {
		return requireAlias;
	}

	@Nullable
	public Duration getTimeout() {
		return timeout;
	}

	@Nullable
	public Boolean getRefresh() {
		return refresh;
	}

	@Nullable
	public String getWaitForActiveShards() {
		return waitForActiveShards;
	}

	@Nullable
	public Long getRequestsPerSecond() {
		return requestsPerSecond;
	}

	@Nullable
	public Duration getScroll() {
		return scroll;
	}

	@Nullable
	public Long getSlices() {
		return slices;
	}

	public static ReindexRequestBuilder builder(IndexCoordinates sourceIndex, IndexCoordinates destIndex) {
		return new ReindexRequestBuilder(sourceIndex, destIndex);
	}

	public enum Conflicts {
		PROCEED("proceed"), ABORT("abort");

		// value used in Elasticsearch
		private final String esName;

		Conflicts(String esName) {
			this.esName = esName;
		}

		public String getEsName() {
			return esName;
		}
	}

	public static class Source {
		private final IndexCoordinates indexes;
		@Nullable private Query query;
		@Nullable private Remote remote;
		@Nullable private Slice slice;
		@Nullable private Integer size;
		@Nullable private SourceFilter sourceFilter;

		private Source(IndexCoordinates indexes) {
			Assert.notNull(indexes, "indexes must not be null");

			this.indexes = indexes;
		}

		public IndexCoordinates getIndexes() {
			return indexes;
		}

		@Nullable
		public Remote getRemote() {
			return remote;
		}

		@Nullable
		public Query getQuery() {
			return query;
		}

		@Nullable
		public Integer getSize() {
			return size;
		}

		@Nullable
		public Slice getSlice() {
			return slice;
		}

		@Nullable
		public SourceFilter getSourceFilter() {
			return sourceFilter;
		}
	}

	public static class Slice {
		private final int id;
		private final int max;

		private Slice(int id, int max) {
			this.id = id;
			this.max = max;
		}

		public int getId() {
			return id;
		}

		public int getMax() {
			return max;
		}
	}

	public static class Dest {

		private final IndexCoordinates index;
		@Nullable private String pipeline;
		@Nullable private String routing;
		@Nullable private Document.VersionType versionType;
		@Nullable private IndexQuery.OpType opType;

		private Dest(IndexCoordinates index) {
			Assert.notNull(index, "dest index must not be null");

			this.index = index;
		}

		public IndexCoordinates getIndex() {
			return index;
		}

		@Nullable
		public Document.VersionType getVersionType() {
			return versionType;
		}

		@Nullable
		public IndexQuery.OpType getOpType() {
			return opType;
		}

		@Nullable
		public String getPipeline() {
			return pipeline;
		}

		@Nullable
		public String getRouting() {
			return routing;
		}
	}

	public static class Script {
		private final String source;
		@Nullable private final String lang;

		private Script(String source, @Nullable String lang) {
			Assert.notNull(source, "source must not be null");

			this.source = source;
			this.lang = lang;
		}

		public String getSource() {
			return source;
		}

		@Nullable
		public String getLang() {
			return lang;
		}
	}

	public static final class ReindexRequestBuilder {

		private final Source source;
		private final Dest dest;
		@Nullable private Long maxDocs;
		@Nullable private Conflicts conflicts;
		@Nullable private Script script;
		@Nullable private Duration timeout;
		@Nullable private Boolean requireAlias;
		@Nullable private Boolean refresh;
		@Nullable private String waitForActiveShards;
		@Nullable private Long requestsPerSecond;
		@Nullable private Duration scroll;
		@Nullable private Long slices;

		public ReindexRequestBuilder(IndexCoordinates sourceIndex, IndexCoordinates destIndex) {

			Assert.notNull(sourceIndex, "sourceIndex must not be null");
			Assert.notNull(destIndex, "destIndex must not be null");

			this.source = new Source(sourceIndex);
			this.dest = new Dest(destIndex);
		}

		// region setter

		public ReindexRequestBuilder withMaxDocs(@Nullable Long maxDocs) {
			this.maxDocs = maxDocs;
			return this;
		}

		public ReindexRequestBuilder withConflicts(Conflicts conflicts) {
			this.conflicts = conflicts;
			return this;
		}

		public ReindexRequestBuilder withSourceQuery(Query query) {
			this.source.query = query;
			return this;
		}

		public ReindexRequestBuilder withSourceSlice(int id, int max) {
			this.source.slice = new Slice(id, max);
			return this;
		}

		public ReindexRequestBuilder withSourceRemote(Remote remote) {
			this.source.remote = remote;
			return this;
		}

		public ReindexRequestBuilder withSourceSize(int size) {
			this.source.size = size;
			return this;
		}

		public ReindexRequestBuilder withSourceSourceFilter(SourceFilter sourceFilter) {
			this.source.sourceFilter = sourceFilter;
			return this;
		}

		public ReindexRequestBuilder withDestPipeline(String pipelineName) {
			this.dest.pipeline = pipelineName;
			return this;
		}

		public ReindexRequestBuilder withDestRouting(String routing) {
			this.dest.routing = routing;
			return this;
		}

		public ReindexRequestBuilder withDestVersionType(Document.VersionType versionType) {
			this.dest.versionType = versionType;
			return this;
		}

		public ReindexRequestBuilder withDestOpType(IndexQuery.OpType opType) {
			this.dest.opType = opType;
			return this;
		}

		public ReindexRequestBuilder withScript(String source, @Nullable String lang) {
			this.script = new Script(source, lang);
			return this;
		}

		public ReindexRequestBuilder withTimeout(Duration timeout) {
			this.timeout = timeout;
			return this;
		}

		public ReindexRequestBuilder withRequireAlias(boolean requireAlias) {
			this.requireAlias = requireAlias;
			return this;
		}

		public ReindexRequestBuilder withRefresh(boolean refresh) {
			this.refresh = refresh;
			return this;
		}

		public ReindexRequestBuilder withWaitForActiveShards(String waitForActiveShards) {
			this.waitForActiveShards = waitForActiveShards;
			return this;
		}

		public ReindexRequestBuilder withRequestsPerSecond(long requestsPerSecond) {
			this.requestsPerSecond = requestsPerSecond;
			return this;
		}

		public ReindexRequestBuilder withScroll(Duration scroll) {
			this.scroll = scroll;
			return this;
		}

		public ReindexRequestBuilder withSlices(long slices) {
			this.slices = slices;
			return this;
		}
		// endregion

		public ReindexRequest build() {
			return new ReindexRequest(source, dest, maxDocs, conflicts, script, timeout, requireAlias, refresh,
					waitForActiveShards, requestsPerSecond, scroll, slices);
		}
	}
}
