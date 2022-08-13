package org.springframework.data.elasticsearch.repositories.complex.custommethod.autowiring;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

/**
 * @author Artur Konczak
 * @author Peter-Josef Meisch
 */
public class ComplexElasticsearchRepositoryImpl implements ComplexElasticsearchRepositoryCustom {

	@Autowired private ElasticsearchOperations operations;

	@Override
	public String doSomethingSpecial() {

		assertThat(operations.getElasticsearchConverter()).isNotNull();

		return "2+2=4";
	}
}
