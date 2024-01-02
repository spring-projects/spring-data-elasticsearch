/*
 * Copyright 2023-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.repository.support.querybyexample;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

import org.reactivestreams.Publisher;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;

/**
 * @author Ezequiel Antúnez Camacho
 * @since 5.1
 */
public class ReactiveQueryByExampleElasticsearchExecutor<T> implements ReactiveQueryByExampleExecutor<T> {

	protected ReactiveElasticsearchOperations operations;
	protected ExampleCriteriaMapper exampleCriteriaMapper;

	public ReactiveQueryByExampleElasticsearchExecutor(ReactiveElasticsearchOperations operations) {
		this.operations = operations;
		this.exampleCriteriaMapper = new ExampleCriteriaMapper(operations.getElasticsearchConverter().getMappingContext());
	}

	@Override
	public <S extends T> Mono<S> findOne(Example<S> example) {
		return Mono.just(example)
				.map(e -> CriteriaQuery.builder(exampleCriteriaMapper.criteria(e)).withMaxResults(2).build())
				.flatMapMany(criteriaQuery -> operations.search(criteriaQuery, example.getProbeType(),
						operations.getIndexCoordinatesFor(example.getProbeType())))
				.buffer(2).map(searchHitList -> {
					if (searchHitList.size() > 1) {
						throw new IncorrectResultSizeDataAccessException(1);
					}
					return searchHitList.iterator().next();
				}).map(SearchHit::getContent).next();
	}

	@Override
	public <S extends T> Flux<S> findAll(Example<S> example) {
		return Mono.just(example).map(e -> new CriteriaQuery(exampleCriteriaMapper.criteria(e)))
				.flatMapMany(criteriaQuery -> operations.search(criteriaQuery, example.getProbeType(),
						operations.getIndexCoordinatesFor(example.getProbeType())))
				.map(SearchHit::getContent);
	}

	@Override
	public <S extends T> Flux<S> findAll(Example<S> example, Sort sort) {
		return Mono.just(example).map(e -> CriteriaQuery.builder(exampleCriteriaMapper.criteria(e)).withSort(sort).build())
				.flatMapMany(criteriaQuery -> operations.search(criteriaQuery, example.getProbeType(),
						operations.getIndexCoordinatesFor(example.getProbeType())))
				.map(SearchHit::getContent);
	}

	@Override
	public <S extends T> Mono<Long> count(Example<S> example) {
		return Mono.just(example).map(e -> new CriteriaQuery(exampleCriteriaMapper.criteria(e)))
				.flatMap(criteriaQuery -> operations.count(criteriaQuery, example.getProbeType(),
						operations.getIndexCoordinatesFor(example.getProbeType())));
	}

	@Override
	public <S extends T> Mono<Boolean> exists(Example<S> example) {
		return count(example).map(count -> count > 0);

	}

	@Override
	public <S extends T, R, P extends Publisher<R>> P findBy(Example<S> example,
			Function<FluentQuery.ReactiveFluentQuery<S>, P> queryFunction) {
		throw new UnsupportedOperationException("findBy example and queryFunction is not supported");
	}

}
