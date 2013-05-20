package org.springframework.data.elasticsearch.core.facet;

import org.springframework.util.Assert;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Artur Konczak
 * @author Jonathan Yan
 */
public class AbstactFacetResult implements FacetResult {

    private final String name;
    private final FacetType type;

    protected AbstactFacetResult(String name, FacetType type) {
        Assert.hasText(name, "Facet name can't be null and should have a value");
        this.name = name;
        this.type = type;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public FacetType getType() {
        return type;
    }
}
