/*
 * Copyright 2013-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.lang.Nullable;

/**
 * @param <T>
 * @param <ID>
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Sascha Woo
 * @author Murali Chevuri
 * @author Peter-Josef Meisch
 */
@SuppressWarnings("unused")
@NoRepositoryBean
public interface ElasticsearchRepository<T, ID> extends PagingAndSortingRepository<T, ID>, CrudRepository<T, ID> {

	/**
	 * Search for similar entities using a morelikethis query
	 *
	 * @param entity the entity for which similar documents should be searched, must not be {@literal null}
	 * @param fields
	 * @param pageable , must not be {@literal null}
	 * @return
	 */
	Page<T> searchSimilar(T entity, @Nullable String[] fields, Pageable pageable);

	/**
	 * @since 5.2
	 */
	<S extends T> S save(S entity, @Nullable RefreshPolicy refreshPolicy);

	/**
	 * @since 5.2
	 */
	<S extends T> Iterable<S> saveAll(Iterable<S> entities, @Nullable RefreshPolicy refreshPolicy);

	/**
	 * @since 5.2
	 */
	void deleteById(ID id, @Nullable RefreshPolicy refreshPolicy);

	/**
	 * @since 5.2
	 */
	void delete(T entity, @Nullable RefreshPolicy refreshPolicy);

	/**
	 * @since 5.2
	 */
	void deleteAllById(Iterable<? extends ID> ids, @Nullable RefreshPolicy refreshPolicy);

	/**
	 * @since 5.2
	 */
	void deleteAll(Iterable<? extends T> entities, @Nullable RefreshPolicy refreshPolicy);

	/**
	 * @since 5.2
	 */
	void deleteAll(@Nullable RefreshPolicy refreshPolicy);
}
