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
package org.springframework.data.elasticsearch.core.query.highlight;

import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author Peter-Josef Meisch
 * @author Haibo Liu
 * @since 4.3
 */
public abstract class HighlightCommonParameters {
	private final String boundaryChars;
	private final int boundaryMaxScan;
	private final String boundaryScanner;
	private final String boundaryScannerLocale;
	private final boolean forceSource;
	private final String fragmenter;
	private final int fragmentSize;
	private final int noMatchSize;
	private final int numberOfFragments;
	@Nullable private final Query highlightQuery;
	private final String order;
	private final int phraseLimit;
	private final String[] preTags;
	private final String[] postTags;
	private final boolean requireFieldMatch;
	private final String type;

	protected HighlightCommonParameters(HighlightCommonParametersBuilder<?> builder) {

		Assert.notNull(builder, "builder must not be null");

		boundaryChars = builder.boundaryChars;
		boundaryMaxScan = builder.boundaryMaxScan;
		boundaryScanner = builder.boundaryScanner;
		boundaryScannerLocale = builder.boundaryScannerLocale;
		forceSource = builder.forceSource;
		fragmenter = builder.fragmenter;
		fragmentSize = builder.fragmentSize;
		noMatchSize = builder.noMatchSize;
		numberOfFragments = builder.numberOfFragments;
		highlightQuery = builder.highlightQuery;
		order = builder.order;
		phraseLimit = builder.phraseLimit;
		preTags = builder.preTags;
		postTags = builder.postTags;
		requireFieldMatch = builder.requireFieldMatch;
		type = builder.type;
	}

	public String getBoundaryChars() {
		return boundaryChars;
	}

	public int getBoundaryMaxScan() {
		return boundaryMaxScan;
	}

	public String getBoundaryScanner() {
		return boundaryScanner;
	}

	public String getBoundaryScannerLocale() {
		return boundaryScannerLocale;
	}

	public boolean getForceSource() {
		return forceSource;
	}

	public String getFragmenter() {
		return fragmenter;
	}

	public int getFragmentSize() {
		return fragmentSize;
	}

	public int getNoMatchSize() {
		return noMatchSize;
	}

	public int getNumberOfFragments() {
		return numberOfFragments;
	}

	@Nullable
	public Query getHighlightQuery() {
		return highlightQuery;
	}

	public String getOrder() {
		return order;
	}

	public int getPhraseLimit() {
		return phraseLimit;
	}

	public String[] getPreTags() {
		return preTags;
	}

	public String[] getPostTags() {
		return postTags;
	}

	public boolean getRequireFieldMatch() {
		return requireFieldMatch;
	}

	public String getType() {
		return type;
	}

	@SuppressWarnings("unchecked")
	public static abstract class HighlightCommonParametersBuilder<SELF extends HighlightCommonParametersBuilder<SELF>> {
		private String boundaryChars = "";
		private int boundaryMaxScan = -1;
		private String boundaryScanner = "";
		private String boundaryScannerLocale = "";
		private boolean forceSource = false;
		private String fragmenter = "";
		private int fragmentSize = -1;
		private int noMatchSize = -1;
		private int numberOfFragments = -1;
		/**
		 * Only the search query part of the {@link Query} takes effect, others are just ignored.
		 */
		@Nullable private Query highlightQuery = null;
		private String order = "";
		private int phraseLimit = -1;
		private String[] preTags = new String[0];
		private String[] postTags = new String[0];
		private boolean requireFieldMatch = true;
		private String type = "";

		protected HighlightCommonParametersBuilder() {}

		public SELF withBoundaryChars(String boundaryChars) {
			this.boundaryChars = boundaryChars;
			return (SELF) this;
		}

		public SELF withBoundaryMaxScan(int boundaryMaxScan) {
			this.boundaryMaxScan = boundaryMaxScan;
			return (SELF) this;
		}

		public SELF withBoundaryScanner(String boundaryScanner) {
			this.boundaryScanner = boundaryScanner;
			return (SELF) this;
		}

		public SELF withBoundaryScannerLocale(String boundaryScannerLocale) {
			this.boundaryScannerLocale = boundaryScannerLocale;
			return (SELF) this;
		}

		public SELF withForceSource(boolean forceSource) {
			this.forceSource = forceSource;
			return (SELF) this;
		}

		public SELF withFragmenter(String fragmenter) {
			this.fragmenter = fragmenter;
			return (SELF) this;
		}

		public SELF withFragmentSize(int fragmentSize) {
			this.fragmentSize = fragmentSize;
			return (SELF) this;
		}

		public SELF withNoMatchSize(int noMatchSize) {
			this.noMatchSize = noMatchSize;
			return (SELF) this;
		}

		public SELF withNumberOfFragments(int numberOfFragments) {
			this.numberOfFragments = numberOfFragments;
			return (SELF) this;
		}

		public SELF withHighlightQuery(@Nullable Query highlightQuery) {
			this.highlightQuery = highlightQuery;
			return (SELF) this;
		}

		public SELF withOrder(String order) {
			this.order = order;
			return (SELF) this;
		}

		public SELF withPhraseLimit(int phraseLimit) {
			this.phraseLimit = phraseLimit;
			return (SELF) this;
		}

		public SELF withPreTags(String... preTags) {
			this.preTags = preTags;
			return (SELF) this;
		}

		public SELF withPostTags(String... postTags) {
			this.postTags = postTags;
			return (SELF) this;
		}

		public SELF withRequireFieldMatch(boolean requireFieldMatch) {
			this.requireFieldMatch = requireFieldMatch;
			return (SELF) this;
		}

		public SELF withType(String type) {
			this.type = type;
			return (SELF) this;
		}

		public abstract HighlightCommonParameters build();
	}
}
