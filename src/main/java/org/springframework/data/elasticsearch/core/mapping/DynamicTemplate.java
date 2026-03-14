package org.springframework.data.elasticsearch.core.mapping;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable Value object encapsulating dynamic template(s).
 * {@see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/dynamic-templates.html">Elastic
 * docs</a>}
 *
 * @author Youssef Aouichaoui
 * @since 5.4
 */
public class DynamicTemplate {
	/**
	 * Patterns to match on the field name.
	 */
	private final List<String> match;

	/**
	 * Path patterns for a nested type to match the field name.
	 */
	private final List<String> pathMatch;

	/**
	 * Patterns that do not match the field name.
	 */
	private final List<String> unmatch;

	/**
	 * Path patterns for a nested type that do not match the field name.
	 */
	private final List<String> pathUnmatch;

	/**
	 * Data types that correspond to the field.
	 */
	private final List<String> matchMappingType;

	/**
	 * Data types that do not match to the field.
	 */
	private final List<String> unmatchMappingType;

	private DynamicTemplate(Builder builder) {
		this.match = builder.match;
		this.pathMatch = builder.pathMatch;

		this.unmatch = builder.unmatch;
		this.pathUnmatch = builder.pathUnmatch;

		this.matchMappingType = builder.matchMappingType;
		this.unmatchMappingType = builder.unmatchMappingType;
	}

	public List<String> getMatch() {
		return match;
	}

	public List<String> getPathMatch() {
		return pathMatch;
	}

	public List<String> getUnmatch() {
		return unmatch;
	}

	public List<String> getPathUnmatch() {
		return pathUnmatch;
	}

	public List<String> getMatchMappingType() {
		return matchMappingType;
	}

	public List<String> getUnmatchMappingType() {
		return unmatchMappingType;
	}

	public boolean isRegexMatching() {
		return false;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private final List<String> match = new ArrayList<>();
		private final List<String> pathMatch = new ArrayList<>();

		private final List<String> unmatch = new ArrayList<>();
		private final List<String> pathUnmatch = new ArrayList<>();

		private final List<String> matchMappingType = new ArrayList<>();
		private final List<String> unmatchMappingType = new ArrayList<>();

		private Builder() {}

		/**
		 * Patterns to match on the field name.
		 */
		public Builder withMatch(String... match) {
			for (String value : match) {
				if (value != null) {
					parseValues(value, this.match);
				}
			}

			return this;
		}

		/**
		 * Path patterns for a nested type to match the field name.
		 */
		public Builder withPathMatch(String... pathMatch) {
			for (String value : pathMatch) {
				if (value != null) {
					parseValues(value, this.pathMatch);
				}
			}

			return this;
		}

		/**
		 * Patterns that do not match the field name.
		 */
		public Builder withUnmatch(String... unmatch) {
			for (String value : unmatch) {
				if (value != null) {
					parseValues(value, this.unmatch);
				}
			}

			return this;
		}

		/**
		 * Path patterns for a nested type that do not match the field name.
		 */
		public Builder withPathUnmatch(String... pathUnmatch) {
			for (String value : pathUnmatch) {
				if (value != null) {
					parseValues(value, this.pathUnmatch);
				}
			}

			return this;
		}

		/**
		 * Data types that correspond to the field.
		 */
		public Builder withMatchMappingType(String... matchMappingType) {
			for (String value : matchMappingType) {
				if (value != null) {
					parseValues(value, this.matchMappingType);
				}
			}

			return this;
		}

		/**
		 * Data types that do not match to the field.
		 */
		public Builder withUnmatchMappingType(String... unmatchMappingType) {
			for (String value : unmatchMappingType) {
				if (value != null) {
					parseValues(value, this.unmatchMappingType);
				}
			}

			return this;
		}

		private void parseValues(String source, List<String> target) {
			if (source.startsWith("[")) {
				target.addAll(List.of(source.replace("[", "").replace("]", "").split(",", -1)));
			} else {
				target.add(source);
			}
		}

		public DynamicTemplate build() {
			return new DynamicTemplate(this);
		}
	}
}
