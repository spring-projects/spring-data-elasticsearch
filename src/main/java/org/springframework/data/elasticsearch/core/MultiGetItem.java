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
package org.springframework.data.elasticsearch.core;

import org.springframework.data.elasticsearch.ElasticsearchErrorCause;
import org.springframework.lang.Nullable;

/**
 * Response object for items returned from multiget requests, encapsulating the returned data and potential error
 * information.
 *
 * @param <T> the entity type
 * @author Peter-Josef Meisch
 * @since 4.2
 */
public class MultiGetItem<T> {
	@Nullable private final T item;
	@Nullable private final Failure failure;

	private MultiGetItem(@Nullable T item, @Nullable Failure failure) {
		this.item = item;
		this.failure = failure;
	}

	public static <T> MultiGetItem<T> of(@Nullable T item, @Nullable Failure failure) {
		return new MultiGetItem<>(item, failure);
	}

	public boolean hasItem() {
		return item != null;
	}

	@Nullable
	public T getItem() {
		return item;
	}

	public boolean isFailed() {
		return failure != null;
	}

	@Nullable
	public Failure getFailure() {
		return failure;
	}

	public static class Failure {
		@Nullable private final String index;
		@Nullable private final String type;
		@Nullable private final String id;
		@Nullable private final Exception exception;
		@Nullable private final ElasticsearchErrorCause elasticsearchErrorCause;

		private Failure(@Nullable String index, @Nullable String type, @Nullable String id, @Nullable Exception exception,
				@Nullable ElasticsearchErrorCause elasticsearchErrorCause) {
			this.index = index;
			this.type = type;
			this.id = id;
			this.exception = exception;
			this.elasticsearchErrorCause = elasticsearchErrorCause;
		}

		public static Failure of(String index, @Nullable String type, String id, @Nullable Exception exception,
				@Nullable ElasticsearchErrorCause elasticsearchErrorCause) {
			return new Failure(index, type, id, exception, elasticsearchErrorCause);
		}

		@Nullable
		public String getIndex() {
			return index;
		}

		@Nullable
		public String getType() {
			return type;
		}

		@Nullable
		public String getId() {
			return id;
		}

		@Nullable
		public Exception getException() {
			return exception;
		}

		@Nullable
		public ElasticsearchErrorCause getElasticsearchErrorCause() {
			return elasticsearchErrorCause;
		}
	}
}
