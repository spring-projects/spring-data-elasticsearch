package org.springframework.data.elasticsearch.core.query;

/**
 * SourceFilter for providing includes and excludes.
 *
 * @Author Jon Tsiros
 */
public interface SourceFilter {
	String[] getIncludes();

	String[] getExcludes();
}
