/*
 * Copyright2020-2021 the original author or authors.
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
package org.springframework.data.elasticsearch.core.routing;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Routing;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;

/**
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
public abstract class ReactiveRoutingTests {

	private static final String ID_1 = "id1";
	private static final String ID_2 = "id2";
	private static final String ID_3 = "id3";

	@Autowired ReactiveElasticsearchOperations operations;
	@Autowired private IndexNameProvider indexNameProvider;

	@BeforeAll
	static void beforeAll() {
		// check that the used id values go to different shards of the index which is configured to have 5 shards.
		// Elasticsearch uses the following function:
		Function<String, Integer> calcShard = routing -> Math
				.floorMod(RoutingIntegrationTests.Murmur3HashFunction.hash(routing), 5);

		Integer shard1 = calcShard.apply(ID_1);
		Integer shard2 = calcShard.apply(ID_2);
		Integer shard3 = calcShard.apply(ID_3);

		assertThat(shard1).isNotEqualTo(shard2);
		assertThat(shard1).isNotEqualTo(shard3);
		assertThat(shard2).isNotEqualTo(shard3);
	}

	@BeforeEach
	void setUp() {
		indexNameProvider.increment();
		operations.indexOps(RoutingEntity.class).createWithMapping().block();
	}

	@Test
	@Order(java.lang.Integer.MAX_VALUE)
	void cleanup() {
		operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + "*")).delete().block();
	}

	@Test // #1218
	@DisplayName("should store data with different routing and be able to get it")
	void shouldStoreDataWithDifferentRoutingAndBeAbleToGetIt() {

		RoutingEntity entity = new RoutingEntity(ID_1, ID_2);
		operations.save(entity).block();

		RoutingEntity savedEntity = operations.withRouting(RoutingResolver.just(ID_2)).get(entity.id, RoutingEntity.class)
				.block();

		assertThat(savedEntity).isEqualTo(entity);
	}

	@Test // #1218
	@DisplayName("should store data with different routing and be able to delete it")
	void shouldStoreDataWithDifferentRoutingAndBeAbleToDeleteIt() {

		RoutingEntity entity = new RoutingEntity(ID_1, ID_2);
		operations.save(entity).block();

		String deletedId = operations.withRouting(RoutingResolver.just(ID_2))
				.delete(entity.id, IndexCoordinates.of(indexNameProvider.indexName())).block();

		assertThat(deletedId).isEqualTo(entity.getId());
	}

	@Test // #1218
	@DisplayName("should store data with different routing and get the routing in the search result")
	void shouldStoreDataWithDifferentRoutingAndGetTheRoutingInTheSearchResult() {

		RoutingEntity entity = new RoutingEntity(ID_1, ID_2);
		operations.save(entity).block();

		List<SearchHit<RoutingEntity>> searchHits = operations.search(Query.findAll(), RoutingEntity.class).collectList()
				.block();

		assertThat(searchHits).hasSize(1);
		assertThat(searchHits.get(0).getRouting()).isEqualTo(ID_2);
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	@Setting(shards = 5)
	@Routing("routing")
	static class RoutingEntity {
		@Nullable
		@Id private String id;
		@Nullable private String routing;

		public RoutingEntity(@Nullable String id, @Nullable String routing) {
			this.id = id;
			this.routing = routing;
		}

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getRouting() {
			return routing;
		}

		public void setRouting(@Nullable String routing) {
			this.routing = routing;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof RoutingEntity that))
				return false;

			if (!Objects.equals(id, that.id))
				return false;
			return Objects.equals(routing, that.routing);
		}

		@Override
		public int hashCode() {
			int result = id != null ? id.hashCode() : 0;
			result = 31 * result + (routing != null ? routing.hashCode() : 0);
			return result;
		}
	}
}
