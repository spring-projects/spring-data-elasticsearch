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
	@Nullable private Integer ifSeqNo;
	@Nullable private Integer ifPrimaryTerm;

	public static Builder builder(String id) {
		return new Builder(id);
	}

	private UpdateQuery(String id, @Nullable String script, @Nullable Map<String, Object> params,
			@Nullable Document document, @Nullable Document upsert, @Nullable String lang, @Nullable String routing,
			@Nullable Boolean scriptedUpsert, @Nullable Boolean docAsUpsert, @Nullable Boolean fetchSource,
			@Nullable Integer ifSeqNo, @Nullable Integer ifPrimaryTerm) {
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
		this.ifSeqNo = ifSeqNo;
		this.ifPrimaryTerm = ifPrimaryTerm;
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
	public Integer getIfSeqNo() {
		return ifSeqNo;
	}

	@Nullable
	public Integer getIfPrimaryTerm() {
		return ifPrimaryTerm;
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

		public UpdateQuery build() {

			if (script == null && document == null) {
				throw new IllegalArgumentException("either script or document must be set");
			}
			return new UpdateQuery(id, script, params, document, upsert, lang, routing, scriptedUpsert, docAsUpsert,
					fetchSource, ifSeqNo, ifPrimaryTerm);
		}
	}
}
