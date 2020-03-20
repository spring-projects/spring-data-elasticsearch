
package org.springframework.data.elasticsearch.core;

import org.springframework.data.domain.Page;

/**
 * @author Artur Konczak
 * @author Peter-Josef Meisch
 * @author Sascha Woo
 * @deprecated since 4.0, will be removed in a future version.
 */
@Deprecated
public interface ScrolledPage<T> extends Page<T> {

	String getScrollId();
}
