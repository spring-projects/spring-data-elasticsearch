package org.springframework.data.elasticsearch.core;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.facet.Facet;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.facet.DefaultFacetMapper;
import org.springframework.data.elasticsearch.core.facet.FacetResult;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.MappingContext;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Artur Konczak
 */
public class DefaultResultMapper extends AbstractResultMapper {

    private MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext;
   
    public DefaultResultMapper(){
        super(new DefaultEntityMapper());
    }
    
    public DefaultResultMapper(MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext){
       super(new DefaultEntityMapper());
       this.mappingContext = mappingContext;
    }

    public DefaultResultMapper(EntityMapper entityMapper) {
        super(entityMapper);
    }

    @Override
    public <T> FacetedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
        long totalHits = response.getHits().totalHits();
        List<T> results = new ArrayList<T>();
        for (SearchHit hit : response.getHits()) {
            if (hit != null) {
                T result = mapEntity(hit.sourceAsString(), clazz);
                setPersistentEntityId(result, hit.getId(), clazz);
                results.add(result);
            }
        }
        List<FacetResult> facets = new ArrayList<FacetResult>();
        if (response.getFacets() != null) {
            for (Facet facet : response.getFacets()) {
                FacetResult facetResult = DefaultFacetMapper.parse(facet);
                if (facetResult != null) {
                    facets.add(facetResult);
                }
            }
        }

        return new FacetedPageImpl<T>(results, pageable, totalHits, facets);
    }

    @Override
    public <T> T mapResult(GetResponse response, Class<T> clazz) {
        T result = mapEntity(response.getSourceAsString(),clazz);
        if (result != null){
           setPersistentEntityId(result, response.getId(), clazz);
        }
        return result;
    }
    
    private <T> void setPersistentEntityId(T result, String id, Class<T> clazz) {
       if (mappingContext != null && clazz.isAnnotationPresent(Document.class)){
          PersistentProperty<ElasticsearchPersistentProperty> idProperty = mappingContext.getPersistentEntity(clazz).getIdProperty();
          // Only deal with String because ES generated Ids are strings !
          if (idProperty != null && idProperty.getType().isAssignableFrom(String.class)){
              Method setter = idProperty.getSetter();
              if (setter != null){
                  try{
                      setter.invoke(result, id);
                  } catch (Throwable t) {
                      t.printStackTrace();
                  }
              }
          }
      }
    }
}
