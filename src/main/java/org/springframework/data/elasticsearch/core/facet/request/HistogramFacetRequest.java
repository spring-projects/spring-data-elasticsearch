/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core.facet.request;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.elasticsearch.search.facet.FacetBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.histogram.HistogramFacetBuilder;
import org.springframework.data.elasticsearch.core.facet.AbstractFacetRequest;
import org.springframework.util.Assert;

import java.util.concurrent.TimeUnit;

/**
 * @author Artur Konczak
 */
public class HistogramFacetRequest extends AbstractFacetRequest {

    private String field;
    private long interval;
    private TimeUnit timeUnit;

    public HistogramFacetRequest(String name) {
        super(name);
    }

    public void setField(String field) {
        this.field = field;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    public FacetBuilder getFacet() {
        Assert.notNull(getName(), "Facet name can't be a null !!!");
        Assert.isTrue(StringUtils.isNotBlank(field), "Please select field on which to build the facet !!!");
        Assert.isTrue(interval > 0, "Please provide interval as positive value greater them zero !!!");

        HistogramFacetBuilder builder = FacetBuilders.histogramFacet(getName());
        builder.field(field);

        if (timeUnit != null) {
            builder.interval(interval, timeUnit);
        } else {
            builder.interval(interval);
        }

        return builder;
    }
}
