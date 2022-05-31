/*
 * Copyright 2022 the original author or authors.
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
import co.elastic.clients.elasticsearch.core.search.BoundaryScanner;
import co.elastic.clients.elasticsearch.core.search.HighlighterEncoder;
import co.elastic.clients.elasticsearch.core.search.HighlighterFragmenter;
import co.elastic.clients.elasticsearch.core.search.HighlighterOrder;
import co.elastic.clients.elasticsearch.core.search.HighlighterTagsSchema;
import co.elastic.clients.elasticsearch.core.search.HighlighterType;
import co.elastic.clients.elasticsearch.core.search.ScoreMode;

import java.time.Duration;

import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.query.GeoDistanceOrder;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.Order;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.RescorerQuery;
import org.springframework.data.elasticsearch.core.query.UpdateResponse;
import org.springframework.data.elasticsearch.core.reindex.ReindexRequest;
import org.springframework.lang.Nullable;

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
			switch (value.toLowerCase()) {
				case "chars":
					return BoundaryScanner.Chars;
				case "sentence":
					return BoundaryScanner.Sentence;
				case "word":
					return BoundaryScanner.Word;
				default:
					return null;
			}
		}
		return null;
	}

	static Conflicts conflicts(ReindexRequest.Conflicts conflicts) {
		switch (conflicts) {
			case ABORT:
				return Conflicts.Abort;
			case PROCEED:
				return Conflicts.Proceed;
		}

		throw new IllegalArgumentException("Cannot map conflicts value " + conflicts.name());
	}

	@Nullable
	static DistanceUnit distanceUnit(String unit) {

		switch (unit.toLowerCase()) {
			case "in":
			case "inch":
				return DistanceUnit.Inches;
			case "yd":
			case "yards":
				return DistanceUnit.Yards;
			case "ft":
			case "feet":
				return DistanceUnit.Feet;
			case "km":
			case "kilometers":
				return DistanceUnit.Kilometers;
			case "nm":
			case "nmi":
				return DistanceUnit.NauticMiles;
			case "mm":
			case "millimeters":
				return DistanceUnit.Millimeters;
			case "cm":
			case "centimeters":
				return DistanceUnit.Centimeters;
			case "mi":
			case "miles":
				return DistanceUnit.Miles;
			case "m":
			case "meters":
				return DistanceUnit.Meters;
		}
		return null;
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
	static GeoDistanceType geoDistanceType(GeoDistanceOrder.DistanceType distanceType) {

		switch (distanceType) {
			case arc:
				return GeoDistanceType.Arc;
			case plane:
				return GeoDistanceType.Plane;
		}

		return null;
	}

	@Nullable
	static HighlighterFragmenter highlighterFragmenter(@Nullable String value) {

		if (value != null) {
			switch (value.toLowerCase()) {
				case "simple":
					return HighlighterFragmenter.Simple;
				case "span":
					return HighlighterFragmenter.Span;
				default:
					return null;
			}
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
			switch (value.toLowerCase()) {
				case "unified":
					return HighlighterType.Unified;
				case "plain":
					return HighlighterType.Plain;
				case "fvh":
					return HighlighterType.FastVector;
				default:
					return null;
			}
		}

		return null;
	}

	@Nullable
	static HighlighterEncoder highlighterEncoder(@Nullable String value) {

		if (value != null) {
			switch (value.toLowerCase()) {
				case "default":
					return HighlighterEncoder.Default;
				case "html":
					return HighlighterEncoder.Html;
				default:
					return null;
			}
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
			switch (opType) {
				case INDEX:
					return OpType.Index;
				case CREATE:
					return OpType.Create;
			}
		}
		return null;
	}

	static Refresh refresh(@Nullable RefreshPolicy refreshPolicy) {

		if (refreshPolicy == null) {
			return Refresh.False;
		}

		switch (refreshPolicy) {
			case IMMEDIATE:
				return Refresh.True;
			case WAIT_UNTIL:
				return Refresh.WaitFor;
			case NONE:
			default:
				return Refresh.False;
		}
	}

	@Nullable
	static UpdateResponse.Result result(@Nullable Result result) {

		if (result == null) {
			return null;
		}

		switch (result) {
			case Created:
				return UpdateResponse.Result.CREATED;
			case Updated:
				return UpdateResponse.Result.UPDATED;
			case Deleted:
				return UpdateResponse.Result.DELETED;
			case NotFound:
				return UpdateResponse.Result.NOT_FOUND;
			case NoOp:
				return UpdateResponse.Result.NOOP;
		}

		return null;
	}

	@Nullable
	static ScoreMode scoreMode(@Nullable RescorerQuery.ScoreMode scoreMode) {

		if (scoreMode == null) {
			return null;
		}

		switch (scoreMode) {
			case Default:
				return null;
			case Avg:
				return ScoreMode.Avg;
			case Max:
				return ScoreMode.Max;
			case Min:
				return ScoreMode.Min;
			case Total:
				return ScoreMode.Total;
			case Multiply:
				return ScoreMode.Multiply;
		}

		return null;
	}

	@Nullable
	static SearchType searchType(@Nullable Query.SearchType searchType) {

		if (searchType == null) {
			return null;
		}

		switch (searchType) {
			case QUERY_THEN_FETCH:
				return SearchType.QueryThenFetch;
			case DFS_QUERY_THEN_FETCH:
				return SearchType.DfsQueryThenFetch;
		}

		return null;
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

		switch (mode) {
			case min:
				return SortMode.Min;
			case max:
				return SortMode.Max;
			case median:
				return SortMode.Median;
			case avg:
				return SortMode.Avg;
		}

		return null;
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
			switch (versionType) {
				case INTERNAL:
					return VersionType.Internal;
				case EXTERNAL:
					return VersionType.External;
				case EXTERNAL_GTE:
					return VersionType.ExternalGte;
				case FORCE:
					return VersionType.Force;
			}
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

}
