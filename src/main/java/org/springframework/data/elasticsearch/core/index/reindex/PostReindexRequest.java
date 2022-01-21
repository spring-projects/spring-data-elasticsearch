package org.springframework.data.elasticsearch.core.index.reindex;

import org.elasticsearch.index.query.QueryBuilder;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.SourceFilter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.util.CollectionUtils.*;

/**
 * Request to reindex some documents from one index to another.
 * (@see https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-reindex.html)
 *
 * @author Sijia Liu
 */
public class PostReindexRequest {

	// Request body
	private final Source source;
	private final Dest dest;
	@Nullable private final Integer maxDocs;
	@Nullable private final String conflicts;
	@Nullable private final Script script;

	// Query parameters
	@Nullable private final Duration timeout;
	@Nullable private final Boolean requireAlias;
	@Nullable private final Boolean refresh;
	@Nullable private final String waitForActiveShards;
	@Nullable private final Integer requestsPerSecond;
	@Nullable private final Duration scroll;
	@Nullable private final Integer slices;

	PostReindexRequest(@Nullable Integer maxDocs, @Nullable String conflicts, Source source, Dest dest, @Nullable Script script, @Nullable Duration timeout, @Nullable Boolean requireAlias, @Nullable Boolean refresh, @Nullable String waitForActiveShards, @Nullable Integer requestsPerSecond, @Nullable Duration scroll, @Nullable Integer slices) {
		this.maxDocs = maxDocs;
		this.conflicts = conflicts;
		this.source = source;
		this.dest = dest;
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
	public Integer getMaxDocs() {
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
	public String getConflicts() {
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
	public Integer getRequestsPerSecond() {
		return requestsPerSecond;
	}

	@Nullable
	public Duration getScroll() {
		return scroll;
	}

	@Nullable
	public Integer getSlices() {
		return slices;
	}

	public static PostReindexRequestBuilder builder(String sourceIndex, String destIndex) {
		return new PostReindexRequestBuilder(sourceIndex, destIndex);
	}

	public static class Source {
		private final List<String> indexes = new ArrayList<>();
		@Nullable private QueryBuilder query;
		@Nullable private Remote remote;
		@Nullable private Slice slice;
		@Nullable private Integer size;
		@Nullable private SourceFilter sourceFilter;

		public List<String> getIndexes() {
			return indexes;
		}

		@Nullable
		public Remote getRemote() {
			return remote;
		}

		@Nullable
		public QueryBuilder getQuery() {
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
		private final Integer id;
		private final Integer max;

		Slice(Integer id, Integer max) {
			this.id = id;
			this.max = max;
		}

		public Integer getId() {
			return id;
		}

		public Integer getMax() {
			return max;
		}
	}

	public static class Dest {

		private final String index;
		@Nullable private String pipeline;
		@Nullable private String routing;
		@Nullable private Document.VersionType versionType;
		@Nullable private IndexQuery.OpType opType;

		Dest(String index) {
			Assert.notNull(index, "dest index must not be null");
			this.index = index;
		}

		public String getIndex() {
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
		private final String lang;

		Script(String source, String lang) {
			this.source = source;
			this.lang = lang;
		}

		public String getSource() {
			return source;
		}

		public String getLang() {
			return lang;
		}
	}

	public static final class PostReindexRequestBuilder {

		private final Source source;
		private final Dest dest;
		@Nullable private Integer maxDocs;
		@Nullable private String conflicts;
		@Nullable private Script script;
		@Nullable private Duration timeout;
		@Nullable private Boolean requireAlias;
		@Nullable private Boolean refresh;
		@Nullable private String waitForActiveShards;
		@Nullable private Integer requestsPerSecond;
		@Nullable private Duration scroll;
		@Nullable private Integer slices;

		public PostReindexRequestBuilder(String sourceIndex, String destIndex) {
			this.source = new Source();
			this.source.indexes.add(sourceIndex);
			this.dest = new Dest(destIndex);
		}

        // region setter
		public PostReindexRequestBuilder withMaxDocs(@Nullable Integer maxDocs) {
			this.maxDocs = maxDocs;
			return this;
		}

		public PostReindexRequestBuilder withConflicts(String conflicts) {
			this.conflicts = conflicts;
			return this;
		}

		public PostReindexRequestBuilder addSourceIndex(String sourceIndex) {
			Assert.notNull(sourceIndex, "source index must not be null");
			this.source.indexes.add(sourceIndex);
			return this;
		}

		public PostReindexRequestBuilder withSourceIndexes(List<String> sourceIndexes) {
			if (!isEmpty(sourceIndexes)) {
				clearSourceIndexes();
				this.source.indexes.addAll(sourceIndexes);
			}
			return this;
		}

		public PostReindexRequestBuilder clearSourceIndexes() {
			this.source.indexes.clear();
			return this;
		}

		public PostReindexRequestBuilder withSourceQuery(QueryBuilder query) {
			this.source.query = query;
			return this;
		}

		public PostReindexRequestBuilder withSourceSlice(int id, int max){
			this.source.slice = new Slice(id, max);
			return this;
		}

		public PostReindexRequestBuilder withSourceRemote(Remote remote) {
			this.source.remote = remote;
			return this;
		}

		public PostReindexRequestBuilder withSourceSize(int size) {
			this.source.size = size;
			return this;
		}

		public PostReindexRequestBuilder withSourceSourceFilter(SourceFilter sourceFilter){
			this.source.sourceFilter = sourceFilter;
			return this;
		}

		public PostReindexRequestBuilder withDestPipeline(String pipelineName){
			this.dest.pipeline = pipelineName;
			return this;
		}

		public PostReindexRequestBuilder withDestRouting(String routing){
			this.dest.routing = routing;
			return this;
		}

		public PostReindexRequestBuilder withDestVersionType(Document.VersionType versionType) {
			this.dest.versionType = versionType;
			return this;
		}

		public PostReindexRequestBuilder withDestOpType(IndexQuery.OpType opType) {
			this.dest.opType = opType;
			return this;
		}


		public PostReindexRequestBuilder withScript(String source, String lang) {
			this.script = new Script(source, lang);
			return this;
		}

		public PostReindexRequestBuilder withTimeout(Duration timeout){
			this.timeout = timeout;
			return this;
		}

		public PostReindexRequestBuilder withRequireAlias(boolean requireAlias){
			this.requireAlias = requireAlias;
			return this;
		}

		public PostReindexRequestBuilder withRefresh(boolean refresh){
			this.refresh = refresh;
			return this;
		}

		public PostReindexRequestBuilder withWaitForActiveShards(String waitForActiveShards){
			this.waitForActiveShards = waitForActiveShards;
			return this;
		}

		public PostReindexRequestBuilder withRequestsPerSecond(int requestsPerSecond){
			this.requestsPerSecond = requestsPerSecond;
			return this;
		}

		public PostReindexRequestBuilder withScroll(Duration scroll){
			this.scroll = scroll;
			return this;
		}

		public PostReindexRequestBuilder withSlices(int slices){
			this.slices = slices;
			return this;
		}
		// endregion

		public PostReindexRequest build() {
			return new PostReindexRequest(maxDocs, conflicts, source, dest, script, timeout, requireAlias, refresh, waitForActiveShards, requestsPerSecond, scroll, slices);
		}
	}
}
