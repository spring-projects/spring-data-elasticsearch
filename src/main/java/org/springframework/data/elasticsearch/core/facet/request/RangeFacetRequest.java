package org.springframework.data.elasticsearch.core.facet.request;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.search.facet.FacetBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.range.RangeFacetBuilder;
import org.springframework.data.elasticsearch.core.facet.AbstractFacetRequest;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * Range facet for numeric fields
 *
 * @author Artur Konczak
 */
public class RangeFacetRequest extends AbstractFacetRequest {

    private String field;
    private String keyField;
    private String valueField;

    private List<Double> from = new ArrayList<Double>();
    private List<Double> to = new ArrayList<Double>();

    public RangeFacetRequest(String name) {
        super(name);
    }

    public void setField(String field) {
        this.field = field;
    }

    public void setFields(String keyField, String valueField) {
        this.keyField = keyField;
        this.valueField = valueField;
    }

    public void range(Double from, Double to) {
        if (from == null) {
            this.from.add(Double.NEGATIVE_INFINITY);
        } else {
            this.from.add(from);
        }

        if (to == null) {
            this.to.add(Double.POSITIVE_INFINITY);
        } else {
            this.to.add(to);
        }
    }

    @Override
    public FacetBuilder getFacet() {
        Assert.notNull(getName(), "Facet name can't be a null !!!");
        Assert.isTrue(StringUtils.isNotBlank(field) || StringUtils.isNotBlank(keyField) && StringUtils.isNotBlank(valueField), "Please select field or key field and value field !!!");

        RangeFacetBuilder builder = FacetBuilders.rangeFacet(getName());
        if (StringUtils.isNotBlank(keyField)) {
            builder.keyField(keyField).valueField(valueField);
        } else {
            builder.field(field);
        }
        Assert.notEmpty(from, "Please select at last one range");
        Assert.notEmpty(to, "Please select at last one range");
        for (int i = 0; i < from.size(); i++) {
            builder.addRange(from.get(i), to.get(i));
        }
        return builder;
    }
}
