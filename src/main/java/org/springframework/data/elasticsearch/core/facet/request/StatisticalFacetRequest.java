package org.springframework.data.elasticsearch.core.facet.request;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.search.facet.FacetBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.statistical.StatisticalFacetBuilder;
import org.springframework.data.elasticsearch.core.facet.AbstractFacetRequest;
import org.springframework.util.Assert;

/**
 * @author Petar Tahchiev
 */
public class StatisticalFacetRequest extends AbstractFacetRequest {

    private String field;

    private String[] fields;

    public StatisticalFacetRequest(String name) {
        super(name);
    }

    public void setField(String field) {
        this.field = field;
    }

    public void setFields(String... fields) {
        this.fields = fields;
    }

    public FacetBuilder getFacet() {
        Assert.notNull(getName(), "Facet name can't be a null !!!");
        Assert.isTrue(StringUtils.isNotBlank(field) && fields == null, "Please select field or fields on which to build the facets !!!");

        StatisticalFacetBuilder builder = FacetBuilders.statisticalFacet(getName());
        if (ArrayUtils.isNotEmpty(fields)) {
            builder.fields(fields);
        } else {
            builder.field(field);
        }

        return builder;
    }
}
