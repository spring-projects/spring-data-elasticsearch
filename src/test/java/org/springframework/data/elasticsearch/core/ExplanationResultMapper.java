package org.springframework.data.elasticsearch.core;

import org.elasticsearch.search.SearchHit;
import org.springframework.data.elasticsearch.entities.SampleExplanableEntity;

/**
 * Created with IntelliJ IDEA.
 * User: ghiron
 * Date: 1/28/15
 * Time: 4:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExplanationResultMapper extends DefaultResultMapper {
    @Override
    public <T> void mapExplanation(T result, SearchHit hit) {
        if (result instanceof SampleExplanableEntity){
            ((SampleExplanableEntity)result).setExplanation(hit.getExplanation().getDescription());
        }
    }
}
