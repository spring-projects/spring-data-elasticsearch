
package org.springframework.data.elasticsearch.core;

import org.springframework.data.domain.Page;

/**
 * A score-aware page gaining information about max score.
 * 
 * @param <T>
 * @author Sascha Woo
 */
public interface ScoredPage<T> extends Page<T> {

	float getMaxScore();

}
