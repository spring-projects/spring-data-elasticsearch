/*
 * Copyright 2014-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.repositories.complex.custommethod.manualwiring;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.repositories.complex.custommethod.autowiring.ComplexElasticsearchRepositoryCustom;

/**
 * @author Artur Konczak
 * @author Mohsin Husen
 * @author Peter-Josef Meisch
 */
public class ComplexElasticsearchRepositoryManualWiringImpl implements ComplexElasticsearchRepositoryCustom {

	private ElasticsearchOperations operations;

	public ComplexElasticsearchRepositoryManualWiringImpl(ElasticsearchOperations operations) {
		this.operations = operations;
	}

	@Override
	public String doSomethingSpecial() {
		assert (operations.getElasticsearchConverter() != null);
		return "3+3=6";
	}
}
