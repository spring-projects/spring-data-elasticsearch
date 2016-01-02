package org.springframework.data.elasticsearch.core.query;

/**
 * SourceFilter builder for providing includes and excludes.
 *
 * @Author Jon Tsiros
 */
public class FetchSourceFilterBuilder {

	private String[] includes;
	private String[] excludes;

	public FetchSourceFilterBuilder withIncludes(String... includes) {
		this.includes = includes;
		return this;
	}

	public FetchSourceFilterBuilder withExcludes(String... excludes) {
		this.excludes = excludes;
		return this;
	}

	public SourceFilter build() {
		if (includes == null) includes = new String[0];
		if (excludes == null) excludes = new String[0];

		SourceFilter sourceFilter = new FetchSourceFilter(includes, excludes);
		return sourceFilter;
	}
}
