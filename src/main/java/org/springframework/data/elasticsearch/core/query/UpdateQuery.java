/*
 * Copyright 2013-2020 the original author or authors.
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
package org.springframework.data.elasticsearch.core.query;

import java.util.List;
import java.util.Map;

import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.lang.Nullable;

/**
 * Defines an update request.
 * 
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Peter-Josef Meisch
 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-update.html>docs</a>
 */
public class UpdateQuery {

	private String id;
	@Nullable private String script;
	@Nullable private Map<String, Object> params;
	@Nullable private Document document;
	@Nullable private Document upsert;
	@Nullable private String lang;
	@Nullable private String routing;
	@Nullable private Boolean scriptedUpsert;
	@Nullable private Boolean docAsUpsert;
	@Nullable private Boolean fetchSource;
	@Nullable private List<String> fetchSourceIncludes;
	@Nullable private List<String> fetchSourceExcludes;
	@Nullable private Integer ifSeqNo;
	@Nullable private Integer ifPrimaryTerm;
	@Nullable private Refresh refresh;
	@Nullable private Integer retryOnConflict;
	@Nullable String timeout;
	@Nullable String waitForActiveShards;

	public static Builder builder(String id) {
		return new Builder(id);
	}

	private UpdateQuery(String id, @Nullable String script, @Nullable Map<String, Object> params,
			@Nullable Document document, @Nullable Document upsert, @Nullable String lang, @Nullable String routing,
			@Nullable Boolean scriptedUpsert, @Nullable Boolean docAsUpsert, @Nullable Boolean fetchSource,
			@Nullable List<String> fetchSourceIncludes, @Nullable List<String> fetchSourceExcludes, @Nullable Integer ifSeqNo,
			@Nullable Integer ifPrimaryTerm, @Nullable Refresh refresh, @Nullable Integer retryOnConflict,
			@Nullable String timeout, @Nullable String waitForActiveShards) {

		this.id = id;
		this.script = script;
		this.params = params;
		this.document = document;
		this.upsert = upsert;
		this.lang = lang;
		this.routing = routing;
		this.scriptedUpsert = scriptedUpsert;
		this.docAsUpsert = docAsUpsert;
		this.fetchSource = fetchSource;
		this.fetchSourceIncludes = fetchSourceIncludes;
		this.fetchSourceExcludes = fetchSourceExcludes;
		this.ifSeqNo = ifSeqNo;
		this.ifPrimaryTerm = ifPrimaryTerm;
		this.refresh = refresh;
		this.retryOnConflict = retryOnConflict;
		this.timeout = timeout;
		this.waitForActiveShards = waitForActiveShards;
	}

	public String getId() {
		return id;
	}

	@Nullable
	public String getScript() {
		return script;
	}

	@Nullable
	public Map<String, Object> getParams() {
		return params;
	}

	@Nullable
	public Document getDocument() {
		return document;
	}

	@Nullable
	public Document getUpsert() {
		return upsert;
	}

	@Nullable
	public String getLang() {
		return lang;
	}

	@Nullable
	public String getRouting() {
		return routing;
	}

	@Nullable
	public Boolean getScriptedUpsert() {
		return scriptedUpsert;
	}

	@Nullable
	public Boolean getDocAsUpsert() {
		return docAsUpsert;
	}

	@Nullable
	public Boolean getFetchSource() {
		return fetchSource;
	}

	@Nullable
	public List<String> getFetchSourceIncludes() {
		return fetchSourceIncludes;
	}

	@Nullable
	public List<String> getFetchSourceExcludes() {
		return fetchSourceExcludes;
	}

	@Nullable
	public Integer getIfSeqNo() {
		return ifSeqNo;
	}

	@Nullable
	public Integer getIfPrimaryTerm() {
		return ifPrimaryTerm;
	}

	@Nullable
	public Refresh getRefresh() {
		return refresh;
	}

	@Nullable
	public Integer getRetryOnConflict() {
		return retryOnConflict;
	}

	@Nullable
	public String getTimeout() {
		return timeout;
	}

	@Nullable
	public String getWaitForActiveShards() {
		return waitForActiveShards;
	}

	public static final class Builder {
		private String id;
		@Nullable private String script = null;
		@Nullable private Map<String, Object> params;
		@Nullable private Document document = null;
		@Nullable private Document upsert = null;
		@Nullable private String lang = "painless";
		@Nullable private String routing = null;
		@Nullable private Boolean scriptedUpsert;
		@Nullable private Boolean docAsUpsert;
		@Nullable private Boolean fetchSource;
		@Nullable private Integer ifSeqNo;
		@Nullable private Integer ifPrimaryTerm;
		@Nullable private Refresh refresh;
		@Nullable private Integer retryOnConflict;
		@Nullable private String timeout;
		@Nullable String waitForActiveShards;
		@Nullable private List<String> fetchSourceIncludes;
		@Nullable private List<String> fetchSourceExcludes;

		private Builder(String id) {
			this.id = id;
		}

		public Builder withScript(String script) {
			this.script = script;
			return this;
		}

		public Builder withParams(Map<String, Object> params) {
			this.params = params;
			return this;
		}

		public Builder withDocument(Document document) {
			this.document = document;
			return this;
		}

		public Builder withUpsert(Document upsert) {
			this.upsert = upsert;
			return this;
		}

		public Builder withLang(String lang) {
			this.lang = lang;
			return this;
		}

		public Builder withRouting(String routing) {
			this.routing = routing;
			return this;
		}

		public Builder withScriptedUpsert(Boolean scriptedUpsert) {
			this.scriptedUpsert = scriptedUpsert;
			return this;
		}

		public Builder withDocAsUpsert(Boolean docAsUpsert) {
			this.docAsUpsert = docAsUpsert;
			return this;
		}

		public Builder withFetchSource(Boolean fetchSource) {
			this.fetchSource = fetchSource;
			return this;
		}

		public Builder withIfSeqNo(Integer ifSeqNo) {
			this.ifSeqNo = ifSeqNo;
			return this;
		}

		public Builder withIfPrimaryTerm(Integer ifPrimaryTerm) {
			this.ifPrimaryTerm = ifPrimaryTerm;
			return this;
		}

		public Builder withRefresh(Refresh refresh) {
			this.refresh = refresh;
			return this;
		}

		public Builder withRetryOnConflict(Integer retryOnConflict) {
			this.retryOnConflict = retryOnConflict;
			return this;
		}

		public Builder withTimeout(String timeout) {
			this.timeout = timeout;
			return this;
		}

		public Builder withWaitForActiveShards(String waitForActiveShards) {
			this.waitForActiveShards = waitForActiveShards;
			return this;
		}

		public Builder withFetchSourceIncludes(List<String> fetchSourceIncludes) {
			this.fetchSourceIncludes = fetchSourceIncludes;
			return this;
		}

		public Builder withFetchSourceExcludes(List<String> fetchSourceExcludes) {
			this.fetchSourceExcludes = fetchSourceExcludes;
			return this;
		}

		public UpdateQuery build() {

			if (script == null && document == null) {
				throw new IllegalArgumentException("either script or document must be set");
			}
			return new UpdateQuery(id, script, params, document, upsert, lang, routing, scriptedUpsert, docAsUpsert,
					fetchSource, fetchSourceIncludes, fetchSourceExcludes, ifSeqNo, ifPrimaryTerm, refresh, retryOnConflict,
					timeout, waitForActiveShards);
		}
	}

	/*
	 * names will be lowercased on building the query.
	 */
	public enum Refresh {
		True, False, Wait_For
	}
}
