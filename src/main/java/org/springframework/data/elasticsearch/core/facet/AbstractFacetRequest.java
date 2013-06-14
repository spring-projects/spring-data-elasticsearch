package org.springframework.data.elasticsearch.core.facet;

import org.springframework.util.Assert;

/**
 * @author Artur Konczak
 */
public abstract class AbstractFacetRequest implements FacetRequest {

    private String name;
    private boolean applyQueryFilter;

    public AbstractFacetRequest(String name) {
        Assert.hasText(name, "Facet can't be null or empty !!!");
        this.name = name;
    }

    protected String getName(){
        return name;
    }

    public void setApplyQueryFilter(boolean applyQueryFilter) {
        this.applyQueryFilter = applyQueryFilter;
    }

    @Override
    public boolean applyQueryFilter() {
        return applyQueryFilter;
    }
}
