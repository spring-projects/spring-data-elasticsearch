
package org.springframework.data.elasticsearch.core;

import org.springframework.data.domain.Page;
import org.springframework.lang.Nullable;

/**
 * @author Artur Konczak
 * @author Peter-Josef Meisch
 */
public interface ScrolledPage<T> extends Page<T> {

    @Nullable
    String getScrollId();
}
