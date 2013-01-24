package org.springframework.data.elasticsearch.repository.query;


import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.repository.query.parser.ElasticsearchQueryCreator;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.parser.PartTree;

public class ElasticsearchPartQuery extends AbstractElasticsearchRepositoryQuery{

    private final PartTree tree;
    private final MappingContext<?, ElasticsearchPersistentProperty> mappingContext;


    public ElasticsearchPartQuery(ElasticsearchQueryMethod method, ElasticsearchOperations elasticsearchOperations) {
        super(method, elasticsearchOperations);
        this.tree = new PartTree(method.getName(), method.getEntityInformation().getJavaType());
        this.mappingContext = elasticsearchOperations.getElasticsearchConverter().getMappingContext();
    }

    @Override
    public Object execute(Object[] parameters) {
        ParametersParameterAccessor accessor = new ParametersParameterAccessor(queryMethod.getParameters(), parameters);
        CriteriaQuery query = createQuery(accessor);
        if(queryMethod.isPageQuery()){
            return  elasticsearchOperations.queryForPage(query, queryMethod.getEntityInformation().getJavaType());
        }
        return elasticsearchOperations.queryForObject(query, queryMethod.getEntityInformation().getJavaType());
    }

    public CriteriaQuery createQuery(ParametersParameterAccessor accessor) {
        return new ElasticsearchQueryCreator(tree, accessor, mappingContext).createQuery();
    }
}
