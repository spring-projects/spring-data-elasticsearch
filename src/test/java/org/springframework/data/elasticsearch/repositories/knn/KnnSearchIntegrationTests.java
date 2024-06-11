/*
 * Copyright 2024 the original author or authors.
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
package org.springframework.data.elasticsearch.repositories.knn;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.elasticsearch.annotations.FieldType.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.KnnAlgorithmType;
import org.springframework.data.elasticsearch.annotations.KnnIndexOptions;
import org.springframework.data.elasticsearch.annotations.KnnSimilarity;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;

/**
 * @author Haibo Liu
 * @since 5.4
 */
@SpringIntegrationTest
public abstract class KnnSearchIntegrationTests {

	@Autowired ElasticsearchOperations operations;
	@Autowired private IndexNameProvider indexNameProvider;
	@Autowired private VectorEntityRepository vectorEntityRepository;

	@BeforeEach
	public void before() {
		indexNameProvider.increment();
		operations.indexOps(VectorEntity.class).createWithMapping();
	}

	@Test
	@org.junit.jupiter.api.Order(java.lang.Integer.MAX_VALUE)
	void cleanup() {
		operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + "*")).delete();
	}

	private List<VectorEntity> createVectorEntities(int n) {
		List<VectorEntity> entities = new ArrayList<>();
		float increment = 1.0f / n;
		for (int i = 0; i < n; i++) {
			VectorEntity entity = new VectorEntity();
			entity.setId(UUID.randomUUID().toString());
			entity.setMessage("top" + (i + 1));

			// The generated vector is always in the first quadrant, from the x-axis direction to the y-axis direction
			float[] vector = new float[] { 1.0f - i * increment, increment };
			entity.setVector(vector);
			entities.add(entity);
		}

		return entities;
	}

	@Test
	public void shouldReturnXAxisVector() {

		// given
		List<VectorEntity> entities = createVectorEntities(5);
		vectorEntityRepository.saveAll(entities);
		List<Float> xAxisVector = List.of(100f, 0f);

		// when
		NativeQuery query = new NativeQueryBuilder()
				.withKnnSearches(ksb -> ksb.queryVector(xAxisVector).k(3).field("vector"))
				.withPageable(Pageable.ofSize(2))
				.build();
		SearchHits<VectorEntity> result = operations.search(query, VectorEntity.class);

		List<VectorEntity> vectorEntities = result.getSearchHits().stream().map(SearchHit::getContent).toList();

		// then
		assertThat(result).isNotNull();
		assertThat(result.getTotalHits()).isEqualTo(3L);
		// should return the first vector, because it's near x-axis
		assertThat(vectorEntities.get(0).getMessage()).isEqualTo("top1");
	}

	@Test
	public void shouldReturnYAxisVector() {

		// given
		List<VectorEntity> entities = createVectorEntities(10);
		vectorEntityRepository.saveAll(entities);
		List<Float> yAxisVector = List.of(0f, 100f);

		// when
		NativeQuery query = new NativeQueryBuilder()
				.withKnnSearches(ksb -> ksb.queryVector(yAxisVector).k(3).field("vector"))
				.withPageable(Pageable.ofSize(2))
				.build();
		SearchHits<VectorEntity> result = operations.search(query, VectorEntity.class);

		List<VectorEntity> vectorEntities = result.getSearchHits().stream().map(SearchHit::getContent).toList();

		// then
		assertThat(result).isNotNull();
		assertThat(result.getTotalHits()).isEqualTo(3L);
		// should return the last vector, because it's near y-axis
		assertThat(vectorEntities.get(0).getMessage()).isEqualTo("top10");
	}

	public interface VectorEntityRepository extends ElasticsearchRepository<VectorEntity, String> {}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class VectorEntity {
		@Nullable
		@Id private String id;

		@Nullable
		@Field(type = Keyword) private String message;

		// TODO: `elementType = FieldElementType.FLOAT,` is to be added here later
		// TODO: element_type can not be set here, because it's left out in elasticsearch-specification
		// TODO: the issue is fixed in https://github.com/elastic/elasticsearch-java/pull/800, but still not released in
		// 8.13.x
		// TODO: will be fixed later by either upgrading to 8.14.0 or a newer 8.13.x
		@Field(type = FieldType.Dense_Vector, dims = 2,
				knnIndexOptions = @KnnIndexOptions(type = KnnAlgorithmType.HNSW, m = 16, efConstruction = 100),
				knnSimilarity = KnnSimilarity.COSINE) private float[] vector;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getMessage() {
			return message;
		}

		public void setMessage(@Nullable String message) {
			this.message = message;
		}

		@Nullable
		public float[] getVector() {
			return vector;
		}

		public void setVector(@Nullable float[] vector) {
			this.vector = vector;
		}
	}
}
