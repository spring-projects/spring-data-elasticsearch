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
package org.springframework.data.elasticsearch;

import org.springframework.lang.Nullable;

import java.util.List;

/**
 * Object describing an Elasticsearch error
 *
 * @author Peter-Josef Meisch
 * @since 4.4
 */
public class ElasticsearchErrorCause {
	@Nullable
	private final String type;

	private final String reason;

	@Nullable private final String stackTrace;

	@Nullable private final ElasticsearchErrorCause causedBy;

	private final List<ElasticsearchErrorCause> rootCause;

	private final List<ElasticsearchErrorCause> suppressed;

	public ElasticsearchErrorCause(@Nullable String type, String reason, @Nullable String stackTrace,
			@Nullable ElasticsearchErrorCause causedBy, List<ElasticsearchErrorCause> rootCause,
			List<ElasticsearchErrorCause> suppressed) {
		this.type = type;
		this.reason = reason;
		this.stackTrace = stackTrace;
		this.causedBy = causedBy;
		this.rootCause = rootCause;
		this.suppressed = suppressed;
	}

	@Nullable
	public String getType() {
		return type;
	}

	public String getReason() {
		return reason;
	}

	@Nullable
	public String getStackTrace() {
		return stackTrace;
	}

	@Nullable
	public ElasticsearchErrorCause getCausedBy() {
		return causedBy;
	}

	public List<ElasticsearchErrorCause> getRootCause() {
		return rootCause;
	}

	public List<ElasticsearchErrorCause> getSuppressed() {
		return suppressed;
	}
}
