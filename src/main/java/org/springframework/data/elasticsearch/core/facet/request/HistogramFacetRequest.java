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
