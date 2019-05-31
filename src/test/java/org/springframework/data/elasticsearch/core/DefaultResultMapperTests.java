/*
 * Copyright 2013-2019 the original author or authors.
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
package org.springframework.data.elasticsearch.core;

import static java.util.Arrays.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.data.elasticsearch.annotations.FieldType.*;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.lang.Double;
import java.lang.Long;
import java.lang.Object;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.document.DocumentField;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.Score;
import org.springframework.data.elasticsearch.annotations.ScriptedField;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;

import com.fasterxml.jackson.databind.util.ArrayIterator;

/**
 * @author Artur Konczak
 * @author Mohsin Husen
 * @author Chris White
 * @author Mark Paluch
 * @author Ilkang Na
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 */
@RunWith(Parameterized.class)
public class DefaultResultMapperTests {

	private DefaultResultMapper resultMapper;
	private SimpleElasticsearchMappingContext context;
	private EntityMapper entityMapper;

	@Mock private SearchResponse response;

	public DefaultResultMapperTests(SimpleElasticsearchMappingContext context, EntityMapper entityMapper) {

		this.context = context;
		this.entityMapper = entityMapper;
	}

	@Parameters
	public static Collection<Object[]> data() {

		SimpleElasticsearchMappingContext context = new SimpleElasticsearchMappingContext();

		return Arrays.asList(new Object[] { context, new DefaultEntityMapper(context) },
				new Object[] { context, new ElasticsearchEntityMapper(context, new DefaultConversionService()) });
	}

	@Before
	public void init() {

		MockitoAnnotations.initMocks(this);
		resultMapper = new DefaultResultMapper(context, entityMapper);
	}

	@Test
	public void shouldMapAggregationsToPage() {

		// given
		SearchHit[] hits = { createCarHit("Ford", "Grat"), createCarHit("BMW", "Arrow") };
		SearchHits searchHits = mock(SearchHits.class);
		when(searchHits.getTotalHits()).thenReturn(2L);
		when(searchHits.iterator()).thenReturn(new ArrayIterator(hits));
		when(response.getHits()).thenReturn(searchHits);

		Aggregations aggregations = new Aggregations(asList(createCarAggregation()));
		when(response.getAggregations()).thenReturn(aggregations);

		// when
		AggregatedPage<Car> page = resultMapper.mapResults(response, Car.class, Pageable.unpaged());

		// then
		page.hasFacets();
		assertThat(page.hasAggregations()).isTrue();
		assertThat(page.getAggregation("Diesel").getName()).isEqualTo("Diesel");
	}

	@Test
	public void shouldMapSearchRequestToPage() {

		// given
		SearchHit[] hits = { createCarHit("Ford", "Grat"), createCarHit("BMW", "Arrow") };
		SearchHits searchHits = mock(SearchHits.class);
		when(searchHits.getTotalHits()).thenReturn(2L);
		when(searchHits.iterator()).thenReturn(new ArrayIterator(hits));
		when(response.getHits()).thenReturn(searchHits);

		// when
		Page<Car> page = resultMapper.mapResults(response, Car.class, Pageable.unpaged());

		// then
		assertThat(page.hasContent()).isTrue();
		assertThat(page.getTotalElements()).isEqualTo(2);
		assertThat(page.getContent().get(0).getName()).isEqualTo("Ford");
	}

	@Test
	public void shouldMapPartialSearchRequestToObject() {

		// given
		SearchHit[] hits = { createCarPartialHit("Ford", "Grat"), createCarPartialHit("BMW", "Arrow") };
		SearchHits searchHits = mock(SearchHits.class);
		when(searchHits.getTotalHits()).thenReturn(2L);
		when(searchHits.iterator()).thenReturn(new ArrayIterator(hits));
		when(response.getHits()).thenReturn(searchHits);

		// when
		Page<Car> page = resultMapper.mapResults(response, Car.class, Pageable.unpaged());

		// then
		assertThat(page.hasContent()).isTrue();
		assertThat(page.getTotalElements()).isEqualTo(2);
		assertThat(page.getContent().get(0).getName()).isEqualTo("Ford");
	}

	@Test
	public void shouldMapGetRequestToObject() {

		// given
		GetResponse response = mock(GetResponse.class);
		when(response.getSourceAsString()).thenReturn(createJsonCar("Ford", "Grat"));

		// when
		Car result = resultMapper.mapResult(response, Car.class);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getModel()).isEqualTo("Grat");
		assertThat(result.getName()).isEqualTo("Ford");
	}

	@Test // DATAES-281
	@Ignore("fix me - UnsupportedOperation")
	public void setsIdentifierOnImmutableType() {

		GetResponse response = mock(GetResponse.class);
		when(response.getSourceAsString()).thenReturn("{}");
		when(response.getId()).thenReturn("identifier");

		ImmutableEntity result = resultMapper.mapResult(response, ImmutableEntity.class);

		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo("identifier");
	}

	@Test // DATAES-198
	public void setsVersionFromGetResponse() {

		GetResponse response = mock(GetResponse.class);
		when(response.getSourceAsString()).thenReturn("{}");
		when(response.getVersion()).thenReturn(1234L);

		MappedEntity result = resultMapper.mapResult(response, MappedEntity.class);

		assertThat(result).isNotNull();
		assertThat(result.getVersion()).isEqualTo(1234);
	}

	@Test // DATAES-198
	public void setsVersionFromMultiGetResponse() {

		GetResponse response1 = mock(GetResponse.class);
		when(response1.getSourceAsString()).thenReturn("{}");
		when(response1.isExists()).thenReturn(true);
		when(response1.getVersion()).thenReturn(1234L);

		GetResponse response2 = mock(GetResponse.class);
		when(response2.getSourceAsString()).thenReturn("{}");
		when(response2.isExists()).thenReturn(true);
		when(response2.getVersion()).thenReturn(5678L);

		MultiGetResponse multiResponse = mock(MultiGetResponse.class);
		when(multiResponse.getResponses()).thenReturn(new MultiGetItemResponse[] {
				new MultiGetItemResponse(response1, null), new MultiGetItemResponse(response2, null) });

		List<MappedEntity> results = resultMapper.mapResults(multiResponse, MappedEntity.class);

		assertThat(results).isNotNull().hasSize(2);

		assertThat(results.get(0).getVersion()).isEqualTo(1234);
		assertThat(results.get(1).getVersion()).isEqualTo(5678);
	}

	@Test // DATAES-198
	public void setsVersionFromSearchResponse() {

		SearchHit hit1 = mock(SearchHit.class);
		when(hit1.getSourceAsString()).thenReturn("{}");
		when(hit1.getVersion()).thenReturn(1234L);

		SearchHit hit2 = mock(SearchHit.class);
		when(hit2.getSourceAsString()).thenReturn("{}");
		when(hit2.getVersion()).thenReturn(5678L);

		SearchHits searchHits = mock(SearchHits.class);
		when(searchHits.getTotalHits()).thenReturn(2L);
		when(searchHits.iterator()).thenReturn(Arrays.asList(hit1, hit2).iterator());

		SearchResponse searchResponse = mock(SearchResponse.class);
		when(searchResponse.getHits()).thenReturn(searchHits);

		AggregatedPage<MappedEntity> results = resultMapper.mapResults(searchResponse, MappedEntity.class,
				mock(Pageable.class));

		assertThat(results).isNotNull();

		assertThat(results.getContent().get(0).getVersion()).isEqualTo(1234);
		assertThat(results.getContent().get(1).getVersion()).isEqualTo(5678);
	}

	private Aggregation createCarAggregation() {

		Aggregation aggregation = mock(Terms.class);
		when(aggregation.getName()).thenReturn("Diesel");
		return aggregation;
	}

	private SearchHit createCarHit(String name, String model) {

		SearchHit hit = mock(SearchHit.class);
		when(hit.getSourceAsString()).thenReturn(createJsonCar(name, model));
		return hit;
	}

	private SearchHit createCarPartialHit(String name, String model) {

		SearchHit hit = mock(SearchHit.class);
		when(hit.getSourceAsString()).thenReturn(null);
		when(hit.getFields()).thenReturn(createCarFields(name, model));
		return hit;
	}

	private String createJsonCar(String name, String model) {

		String q = "\"";
		StringBuffer sb = new StringBuffer();
		sb.append("{").append(q).append("name").append(q).append(":").append(q).append(name).append(q).append(",");
		sb.append(q).append("model").append(q).append(":").append(q).append(model).append(q).append("}");
		return sb.toString();
	}

	private Map<String, DocumentField> createCarFields(String name, String model) {

		Map<String, DocumentField> result = new HashMap<>();
		result.put("name", new DocumentField("name", asList(name)));
		result.put("model", new DocumentField("model", asList(model)));
		return result;
	}

	@Document(indexName = "test-index-immutable-internal")
	@NoArgsConstructor(force = true)
	@Getter
	static class ImmutableEntity {

		private final String id, name;
	}

	@Data
	static class Car {

		private String name;
		private String model;
	}

	@Data
	@Document(indexName = "test-index-sample-default-result-mapper", type = "test-type")
	static class MappedEntity {

		@Id private String id;
		@Field(type = Text, store = true, fielddata = true) private String type;
		@Field(type = Text, store = true, fielddata = true) private String message;
		private int rate;
		@ScriptedField private Double scriptedRate;
		private boolean available;
		private String highlightedMessage;
		private GeoPoint location;
		@Version private Long version;
		@Score private float score;
	}

}
