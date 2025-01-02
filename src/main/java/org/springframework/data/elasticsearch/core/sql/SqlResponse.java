/*
 * Copyright 2024-2025 the original author or authors.
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
package org.springframework.data.elasticsearch.core.sql;

import static java.util.Collections.*;

import jakarta.json.JsonValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * Defines an SQL response.
 *
 * @author Aouichaoui Youssef
 * @see <a href= "https://www.elastic.co/guide/en/elasticsearch/reference/current/sql-search-api.html">docs</a>
 * @since 5.4
 */
public class SqlResponse {
	/**
	 * If {@code true}, the search is still running.
	 */
	private final boolean running;

	/**
	 * If {@code true}, the response does not contain complete search results.
	 */
	private final boolean partial;

	/**
	 * Cursor for the next set of paginated results.
	 */
	@Nullable private final String cursor;

	/**
	 * Column headings for the search results.
	 */
	private final List<Column> columns;

	/**
	 * Values for the search results.
	 */
	private final List<Row> rows;

	private SqlResponse(Builder builder) {
		this.running = builder.running;
		this.partial = builder.partial;

		this.cursor = builder.cursor;

		this.columns = unmodifiableList(builder.columns);
		this.rows = unmodifiableList(builder.rows);
	}

	public boolean isRunning() {
		return running;
	}

	public boolean isPartial() {
		return partial;
	}

	@Nullable
	public String getCursor() {
		return cursor;
	}

	public List<Column> getColumns() {
		return columns;
	}

	public List<Row> getRows() {
		return rows;
	}

	public static Builder builder() {
		return new Builder();
	}

	public record Column(String name, String type) {
	}

	public static class Row implements Iterable<Map.Entry<Column, JsonValue>> {
		private final Map<Column, JsonValue> row;

		private Row(Builder builder) {
			this.row = builder.row;
		}

		public static Builder builder() {
			return new Builder();
		}

		@NonNull
		@Override
		public Iterator<Map.Entry<Column, JsonValue>> iterator() {
			return row.entrySet().iterator();
		}

		@Nullable
		public JsonValue get(Column column) {
			return row.get(column);
		}

		public static class Builder {
			private final Map<Column, JsonValue> row = new HashMap<>();

			public Builder withValue(Column column, JsonValue value) {
				this.row.put(column, value);

				return this;
			}

			public Row build() {
				return new Row(this);
			}
		}
	}

	public static class Builder {
		private boolean running;
		private boolean partial;

		@Nullable private String cursor;

		private final List<Column> columns = new ArrayList<>();
		private final List<Row> rows = new ArrayList<>();

		private Builder() {}

		/**
		 * If {@code true}, the search is still running.
		 */
		public Builder withRunning(boolean running) {
			this.running = running;

			return this;
		}

		/**
		 * If {@code true}, the response does not contain complete search results.
		 */
		public Builder withPartial(boolean partial) {
			this.partial = partial;

			return this;
		}

		/**
		 * Cursor for the next set of paginated results.
		 */
		public Builder withCursor(@Nullable String cursor) {
			this.cursor = cursor;

			return this;
		}

		/**
		 * Column headings for the search results.
		 */
		public Builder withColumns(List<Column> columns) {
			this.columns.addAll(columns);

			return this;
		}

		/**
		 * Column heading for the search results.
		 */
		public Builder withColumn(Column column) {
			this.columns.add(column);

			return this;
		}

		/**
		 * Values for the search results.
		 */
		public Builder withRows(List<Row> rows) {
			this.rows.addAll(rows);

			return this;
		}

		/**
		 * Value for the search results.
		 */
		public Builder withRow(Row row) {
			this.rows.add(row);

			return this;
		}

		public SqlResponse build() {
			return new SqlResponse(this);
		}
	}
}
