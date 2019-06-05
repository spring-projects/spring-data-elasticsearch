
package org.springframework.data.elasticsearch.core;

import org.springframework.data.domain.Page;

/**
 * @author Artur Konczak
 */
public interface ScrolledPage<T> extends Page<T> {

    String getScrollId();

}
