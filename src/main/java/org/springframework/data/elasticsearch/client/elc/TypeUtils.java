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
package org.springframework.data.elasticsearch.client.elc;

import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.mapping.FieldType;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch.core.search.BoundaryScanner;
import co.elastic.clients.elasticsearch.core.search.HighlighterEncoder;
import co.elastic.clients.elasticsearch.core.search.HighlighterFragmenter;
import co.elastic.clients.elasticsearch.core.search.HighlighterOrder;
import co.elastic.clients.elasticsearch.core.search.HighlighterTagsSchema;
import co.elastic.clients.elasticsearch.core.search.HighlighterType;
import co.elastic.clients.elasticsearch.core.search.ScoreMode;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.json.JsonData;

import java.io.StringReader;
import java.time.Duration;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.query.GeoDistanceOrder;
import org.springframework.data.elasticsearch.core.query.HasChildQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndicesOptions;
import org.springframework.data.elasticsearch.core.query.Order;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.RescorerQuery;
import org.springframework.data.elasticsearch.core.query.UpdateResponse;
import org.springframework.data.elasticsearch.core.query.types.ConflictsType;
import org.springframework.data.elasticsearch.core.query.types.OperatorType;
import org.springframework.data.elasticsearch.core.reindex.ReindexRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Utility to handle new Elasticsearch client type values.
 *
 * @author Peter-Josef Meisch
 * @since 4.4
 */
final class TypeUtils {

	@Nullable
	static BoundaryScanner boundaryScanner(@Nullable String value) {

		if (value != null) {
			return switch (value.toLowerCase()) {
				case "chars" -> BoundaryScanner.Chars;
				case "sentence" -> BoundaryScanner.Sentence;
				case "word" -> BoundaryScanner.Word;
				default -> null;
			};
		}
		return null;
	}

	static Conflicts conflicts(ReindexRequest.Conflicts conflicts) {
		return switch (conflicts) {
			case ABORT -> Conflicts.Abort;
			case PROCEED -> Conflicts.Proceed;
		};
	}

	@Nullable
	static DistanceUnit distanceUnit(String unit) {

		return switch (unit.toLowerCase()) {
			case "in", "inch" -> DistanceUnit.Inches;
			case "yd", "yards" -> DistanceUnit.Yards;
			case "ft", "feet" -> DistanceUnit.Feet;
			case "km", "kilometers" -> DistanceUnit.Kilometers;
			case "nm", "nmi" -> DistanceUnit.NauticMiles;
			case "mm", "millimeters" -> DistanceUnit.Millimeters;
			case "cm", "centimeters" -> DistanceUnit.Centimeters;
			case "mi", "miles" -> DistanceUnit.Miles;
			case "m", "meters" -> DistanceUnit.Meters;
			default -> null;
		};
	}

	@Nullable
	static FieldType fieldType(String type) {

		for (FieldType fieldType : FieldType.values()) {

			if (fieldType.jsonValue().equals(type)) {
				return fieldType;
			}
		}
		return null;
	}

	@Nullable
	static String toString(@Nullable FieldValue fieldValue) {

		if (fieldValue == null) {
			return null;
		}

		switch (fieldValue._kind()) {
			case Double -> {
				return String.valueOf(fieldValue.doubleValue());
			}
			case Long -> {
				return String.valueOf(fieldValue.longValue());
			}
			case Boolean -> {
				return String.valueOf(fieldValue.booleanValue());
			}
			case String -> {
				return fieldValue.stringValue();
			}
			case Null -> {
				return null;
			}
			case Any -> {
				return fieldValue.anyValue().toString();
			}

			default -> throw new IllegalStateException("Unexpected value: " + fieldValue._kind());
		}
	}

	@Nullable
	static Object toObject(@Nullable FieldValue fieldValue) {

		if (fieldValue == null) {
			return null;
		}

		switch (fieldValue._kind()) {
			case Double -> {
				return Double.valueOf(fieldValue.doubleValue());
			}
			case Long -> {
				return Long.valueOf(fieldValue.longValue());
			}
			case Boolean -> {
				return Boolean.valueOf(fieldValue.booleanValue());
			}
			case String -> {
				return fieldValue.stringValue();
			}
			case Null -> {
				return null;
			}
			case Any -> {
				return fieldValue.anyValue().toString();
			}

			default -> throw new IllegalStateException("Unexpected value: " + fieldValue._kind());
		}
	}

	@Nullable
	static FieldValue toFieldValue(@Nullable Object fieldValue) {

		if (fieldValue == null) {
			return FieldValue.NULL;
		}

		if (fieldValue instanceof Boolean b) {
			return b ? FieldValue.TRUE : FieldValue.FALSE;
		}

		if (fieldValue instanceof String s) {
			return FieldValue.of(s);
		}

		if (fieldValue instanceof Long l) {
			return FieldValue.of(l);
		}

		if (fieldValue instanceof Integer i) {
			return FieldValue.of((long) i);
		}

		if (fieldValue instanceof Double d) {
			return FieldValue.of(d);
		}

		if (fieldValue instanceof Float f) {
			return FieldValue.of((double) f);
		}

		return FieldValue.of(JsonData.of(fieldValue));
	}

	@Nullable
	static GeoDistanceType geoDistanceType(GeoDistanceOrder.DistanceType distanceType) {

		return switch (distanceType) {
			case arc -> GeoDistanceType.Arc;
			case plane -> GeoDistanceType.Plane;
		};

	}

	@Nullable
	static SortOrder sortOrder(@Nullable Sort.Direction direction) {

		if (direction == null) {
			return null;
		}

		return switch (direction) {
			case ASC -> SortOrder.Asc;
			case DESC -> SortOrder.Desc;
		};

	}

	@Nullable
	static HighlighterFragmenter highlighterFragmenter(@Nullable String value) {

		if (value != null) {
			return switch (value.toLowerCase()) {
				case "simple" -> HighlighterFragmenter.Simple;
				case "span" -> HighlighterFragmenter.Span;
				default -> null;
			};
		}

		return null;
	}

	@Nullable
	static HighlighterOrder highlighterOrder(@Nullable String value) {

		if (value != null) {
			if ("score".equals(value.toLowerCase())) {
				return HighlighterOrder.Score;
			}
		}

		return null;
	}

	@Nullable
	static HighlighterType highlighterType(@Nullable String value) {

		if (value != null) {
			return switch (value.toLowerCase()) {
				case "unified" -> HighlighterType.Unified;
				case "plain" -> HighlighterType.Plain;
				case "fvh" -> HighlighterType.FastVector;
				default -> null;
			};
		}

		return null;
	}

	@Nullable
	static HighlighterEncoder highlighterEncoder(@Nullable String value) {

		if (value != null) {
			return switch (value.toLowerCase()) {
				case "default" -> HighlighterEncoder.Default;
				case "html" -> HighlighterEncoder.Html;
				default -> null;
			};
		}

		return null;
	}

	@Nullable
	static HighlighterTagsSchema highlighterTagsSchema(@Nullable String value) {

		if (value != null) {
			if ("styled".equals(value.toLowerCase())) {
				return HighlighterTagsSchema.Styled;
			}
		}

		return null;
	}

	@Nullable
	static OpType opType(@Nullable IndexQuery.OpType opType) {

		if (opType != null) {
			return switch (opType) {
				case INDEX -> OpType.Index;
				case CREATE -> OpType.Create;
			};
		}
		return null;
	}

	static Refresh refresh(@Nullable RefreshPolicy refreshPolicy) {

		if (refreshPolicy == null) {
			return Refresh.False;
		}

		return switch (refreshPolicy) {
			case IMMEDIATE -> Refresh.True;
			case WAIT_UNTIL -> Refresh.WaitFor;
			case NONE -> Refresh.False;
		};
	}

	@Nullable
	static UpdateResponse.Result result(@Nullable Result result) {

		if (result == null) {
			return null;
		}

		return switch (result) {
			case Created -> UpdateResponse.Result.CREATED;
			case Updated -> UpdateResponse.Result.UPDATED;
			case Deleted -> UpdateResponse.Result.DELETED;
			case NotFound -> UpdateResponse.Result.NOT_FOUND;
			case NoOp -> UpdateResponse.Result.NOOP;
		};

	}

	@Nullable
	static ScoreMode scoreMode(@Nullable RescorerQuery.ScoreMode scoreMode) {

		if (scoreMode == null) {
			return null;
		}

		return switch (scoreMode) {
			case Default -> null;
			case Avg -> ScoreMode.Avg;
			case Max -> ScoreMode.Max;
			case Min -> ScoreMode.Min;
			case Total -> ScoreMode.Total;
			case Multiply -> ScoreMode.Multiply;
		};

	}

	@Nullable
	static SearchType searchType(@Nullable Query.SearchType searchType) {

		if (searchType == null) {
			return null;
		}

		return switch (searchType) {
			case QUERY_THEN_FETCH -> SearchType.QueryThenFetch;
			case DFS_QUERY_THEN_FETCH -> SearchType.DfsQueryThenFetch;
		};

	}

	@Nullable
	static Slices slices(@Nullable Long count) {

		if (count == null) {
			return null;
		}

		return Slices.of(s -> s.value(Math.toIntExact(count)));
	}

	@Nullable
	static SortMode sortMode(Order.Mode mode) {

		return switch (mode) {
			case min -> SortMode.Min;
			case max -> SortMode.Max;
			case median -> SortMode.Median;
			case avg -> SortMode.Avg;
		};

	}

	@Nullable
	static Time time(@Nullable Duration duration) {

		if (duration == null) {
			return null;
		}

		return Time.of(t -> t.time(duration.toMillis() + "ms"));
	}

	@Nullable
	static String timeStringMs(@Nullable Duration duration) {

		if (duration == null) {
			return null;
		}

		return duration.toMillis() + "ms";
	}

	@Nullable
	static VersionType versionType(
			@Nullable org.springframework.data.elasticsearch.annotations.Document.VersionType versionType) {

		if (versionType != null) {
			return switch (versionType) {
				case INTERNAL -> VersionType.Internal;
				case EXTERNAL -> VersionType.External;
				case EXTERNAL_GTE -> VersionType.ExternalGte;
				case FORCE -> VersionType.Force;
			};
		}

		return null;
	}

	static Integer waitForActiveShardsCount(@Nullable String value) {
		// values taken from the RHLC implementation
		if (value == null) {
			return -2;
		} else if ("all".equals(value.toUpperCase())) {
			return -1;
		} else {
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Illegale value for waitForActiveShards" + value);
			}
		}
	}

	/**
	 * Converts a Long to a Float, returning null if the input is null.
	 *
	 * @param value the long value
	 * @return a FLoat with the given value
	 * @since 5.0
	 */
	@Nullable
	static Float toFloat(@Nullable Long value) {
		return value != null ? Float.valueOf(value) : null;
	}

	/**
	 * @sice 5.1
	 */
	@Nullable
	public static List<ExpandWildcard> expandWildcards(@Nullable EnumSet<IndicesOptions.WildcardStates> wildcardStates) {
		return (wildcardStates != null && !wildcardStates.isEmpty()) ? wildcardStates.stream()
				.map(wildcardState -> ExpandWildcard.valueOf(wildcardState.name().toLowerCase())).collect(Collectors.toList())
				: null;
	}

	@Nullable
	static TypeMapping typeMapping(@Nullable Document mapping) {
		if (mapping != null) {
			return TypeMapping.of(b -> b.withJson(new StringReader(mapping.toJson())));
		}
		return null;
	}

	@Nullable
	static Document typeMapping(@Nullable TypeMapping typeMapping) {
		return (typeMapping != null) ? Document.parse(removePrefixFromJson(typeMapping.toString())) : null;
	}

	public static String removePrefixFromJson(String jsonWithPrefix) {
		return jsonWithPrefix.substring(jsonWithPrefix.indexOf("{"));
	}

	@Nullable
	static IndexSettings indexSettings(@Nullable Map<String, Object> settings) {
		return settings != null ? IndexSettings.of(b -> b.withJson(new StringReader(Document.from(settings).toJson())))
				: null;
	}

	/**
	 * @since 5.2
	 */
	static Map<String, JsonData> paramsMap(Map<String, Object> params) {

		Assert.notNull(params, "params must not be null");

		Map<String, JsonData> mappedParams = new LinkedHashMap<>();
		params.forEach((key, value) -> {
			mappedParams.put(key, JsonData.of(value));
		});
		return mappedParams;
	}

	/**
	 * Convert a spring-data-elasticsearch operator to an Elasticsearch operator.
	 *
	 * @param operator spring-data-elasticsearch operator.
	 * @return an Elasticsearch Operator.
	 * @since 5.3
	 */
	@Nullable
	static Operator operator(@Nullable OperatorType operator) {
		return operator != null ? Operator.valueOf(operator.name()) : null;
	}

	/**
	 * Convert a spring-data-elasticsearch {@literal conflicts} to an Elasticsearch {@literal conflicts}.
	 *
	 * @param conflicts spring-data-elasticsearch {@literal conflicts}.
	 * @return an Elasticsearch {@literal conflicts}.
	 * @since 5.3
	 */
	@Nullable
	static Conflicts conflicts(@Nullable ConflictsType conflicts) {
		return conflicts != null ? Conflicts.valueOf(conflicts.name()) : null;
	}

	/**
	 * Convert a spring-data-elasticsearch {@literal scoreMode} to an Elasticsearch {@literal scoreMode}.
	 *
	 * @param scoreMode spring-data-elasticsearch {@literal scoreMode}.
	 * @return an Elasticsearch {@literal scoreMode}.
	 */
	static ChildScoreMode scoreMode(@Nullable HasChildQuery.ScoreMode scoreMode) {
		if (scoreMode == null) {
			return ChildScoreMode.None;
		}

		return switch (scoreMode) {
			case Avg -> ChildScoreMode.Avg;
			case Max -> ChildScoreMode.Max;
			case Min -> ChildScoreMode.Min;
			case Sum -> ChildScoreMode.Sum;
			default -> ChildScoreMode.None;
		};
	}
}
