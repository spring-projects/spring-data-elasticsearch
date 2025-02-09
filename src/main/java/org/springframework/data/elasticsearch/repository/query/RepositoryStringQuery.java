package org.springframework.data.elasticsearch.repository.query;

import org.springframework.core.convert.ConversionService;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.BaseQuery;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.elasticsearch.repository.support.QueryStringProcessor;
import org.springframework.data.repository.query.ValueExpressionDelegate;
import org.springframework.util.Assert;

/**
 * A repository query that is defined by a String containing the query. Was originally named ElasticsearchStringQuery.
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Mark Paluch
 * @author Taylor Ono
 * @author Peter-Josef Meisch
 * @author Haibo Liu
 */
public class RepositoryStringQuery extends AbstractElasticsearchRepositoryQuery {
	private final String queryString;

	public RepositoryStringQuery(ElasticsearchQueryMethod queryMethod, ElasticsearchOperations elasticsearchOperations,
			String queryString, ValueExpressionDelegate valueExpressionDelegate) {
		super(queryMethod, elasticsearchOperations,
				valueExpressionDelegate.createValueContextProvider(queryMethod.getParameters()));

		Assert.notNull(queryString, "Query cannot be empty");

		this.queryString = queryString;
	}

	@Override
	public boolean isCountQuery() {
		return queryMethod.hasCountQueryAnnotation();
	}

	@Override
	protected boolean isDeleteQuery() {
		return false;
	}

	@Override
	protected boolean isExistsQuery() {
		return false;
	}

	protected BaseQuery createQuery(ElasticsearchParametersParameterAccessor parameterAccessor) {
		ConversionService conversionService = elasticsearchOperations.getElasticsearchConverter().getConversionService();
		var processed = new QueryStringProcessor(queryString, queryMethod, conversionService, evaluationContextProvider)
				.createQuery(parameterAccessor);

		return new StringQuery(processed)
				.addSort(parameterAccessor.getSort());
	}
}
