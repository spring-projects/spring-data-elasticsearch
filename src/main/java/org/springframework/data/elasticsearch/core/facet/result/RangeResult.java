package org.springframework.data.elasticsearch.core.facet.result;

import org.springframework.data.elasticsearch.core.facet.AbstactFacetResult;
import org.springframework.data.elasticsearch.core.facet.FacetType;

import java.util.List;

/**
 * Basic term facet result
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Artur Konczak
 * @author Jonathan Yan
 */
public class RangeResult extends AbstactFacetResult {

    private List<Range> ranges;

    public RangeResult(String name, List<Range> ranges) {
        super(name, FacetType.range);
        this.ranges = ranges;
    }

    public List<Range> getRanges() {
        return ranges;
    }

}
