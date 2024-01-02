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

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchPage;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.data.repository.query.QueryByExampleExecutor;

/**
 * @author Ezequiel Antúnez Camacho
 * @since 5.1
 */
public class QueryByExampleElasticsearchExecutor<T> implements QueryByExampleExecutor<T> {

	protected ElasticsearchOperations operations;
	protected ExampleCriteriaMapper exampleCriteriaMapper;

	public QueryByExampleElasticsearchExecutor(ElasticsearchOperations operations) {
		this.operations = operations;
		this.exampleCriteriaMapper = new ExampleCriteriaMapper(operations.getElasticsearchConverter().getMappingContext());
	}

	@Override
	public <S extends T> Optional<S> findOne(Example<S> example) {
		CriteriaQuery criteriaQuery = CriteriaQuery.builder(exampleCriteriaMapper.criteria(example)).withMaxResults(2).build();
		SearchHits<S> searchHits = operations.search(criteriaQuery, example.getProbeType(),
				operations.getIndexCoordinatesFor(example.getProbeType()));
		if (searchHits.getTotalHits() > 1) {
			throw new org.springframework.dao.IncorrectResultSizeDataAccessException(1);
		}
		return Optional.ofNullable(searchHits).filter(SearchHits::hasSearchHits)
				.map(result -> (List<S>) SearchHitSupport.unwrapSearchHits(result)).map(s -> s.get(0));
	}

	@Override
	public <S extends T> Iterable<S> findAll(Example<S> example) {
		CriteriaQuery criteriaQuery = new CriteriaQuery(exampleCriteriaMapper.criteria(example));
		SearchHits<S> searchHits = operations.search(criteriaQuery, example.getProbeType(),
				operations.getIndexCoordinatesFor(example.getProbeType()));
		return (List<S>) SearchHitSupport.unwrapSearchHits(searchHits);
	}

	@Override
	public <S extends T> Iterable<S> findAll(Example<S> example, Sort sort) {
		CriteriaQuery criteriaQuery = CriteriaQuery.builder(exampleCriteriaMapper.criteria(example)).withSort(sort).build();
		SearchHits<S> searchHits = operations.search(criteriaQuery, example.getProbeType(),
				operations.getIndexCoordinatesFor(example.getProbeType()));
		return (List<S>) SearchHitSupport.unwrapSearchHits(searchHits);
	}

	@Override
	public <S extends T> Page<S> findAll(Example<S> example, Pageable pageable) {
		CriteriaQuery criteriaQuery = CriteriaQuery.builder(exampleCriteriaMapper.criteria(example)).withPageable(pageable)
				.build();
		SearchHits<S> searchHits = operations.search(criteriaQuery, example.getProbeType(),
				operations.getIndexCoordinatesFor(example.getProbeType()));
		SearchPage<S> page = SearchHitSupport.searchPageFor(searchHits, criteriaQuery.getPageable());
		return (Page<S>) SearchHitSupport.unwrapSearchHits(page);

	}

	@Override
	public <S extends T> long count(Example<S> example) {
		final CriteriaQuery criteriaQuery = new CriteriaQuery(exampleCriteriaMapper.criteria(example));
		return operations.count(criteriaQuery, example.getProbeType(),
				operations.getIndexCoordinatesFor(example.getProbeType()));
	}

	@Override
	public <S extends T> boolean exists(Example<S> example) {
		return count(example) > 0L;
	}

	@Override
	public <S extends T, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
		throw new UnsupportedOperationException("findBy example and queryFunction is not supported");
	}

}
