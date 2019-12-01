package org.springframework.data.elasticsearch.repositories.complex.custommethod.autowiring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

/**
 * @author Artur Konczak
 */
public class ComplexElasticsearchRepositoryImpl implements ComplexElasticsearchRepositoryCustom {

	@Autowired private ElasticsearchOperations operations;

	@Override
	public String doSomethingSpecial() {
		assert (operations.getElasticsearchConverter() != null);
		return "2+2=4";
	}
}
