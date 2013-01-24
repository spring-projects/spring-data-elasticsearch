/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core.query;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;

/**
 * AbstractQuery
 * 
 */
abstract class AbstractQuery  implements Query{

    private static final Pageable DEFAULT_PAGE = new PageRequest(0, DEFAULT_PAGE_SIZE);

    protected Pageable pageable = DEFAULT_PAGE;
    protected Sort sort;

    @Override
    public Sort getSort() {
        return this.sort;
    }

    @Override
    public Pageable getPageable() {
        return this.pageable;
    }

    @Override
    public final <T extends Query> T setPageable(Pageable pageable) {
        Assert.notNull(pageable);

        this.pageable = pageable;
        return (T) this.addSort(pageable.getSort());
    }

    @SuppressWarnings("unchecked")
    public final <T extends Query> T addSort(Sort sort) {
        if (sort == null) {
            return (T) this;
        }

        if (this.sort == null) {
            this.sort = sort;
        } else {
            this.sort = this.sort.and(sort);
        }

        return (T) this;
    }

}
