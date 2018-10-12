/*
 * Copyright 2013-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import com.fasterxml.jackson.databind.util.ArrayIterator;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.entities.Car;
import org.springframework.data.elasticsearch.entities.SampleEntity;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Artur Konczak
 * @author Mohsin Husen
 * @author Chris White
 * @author Mark Paluch
 * @author Ilkang Na
 */
public class DefaultResultMapperTests {

	private DefaultResultMapper resultMapper;

	@Mock
	private SearchResponse response;

	@Before
	public void init() {
		MockitoAnnotations.initMocks(this);
		resultMapper = new DefaultResultMapper(new SimpleElasticsearchMappingContext());
	}

	@Test
	public void shouldMapAggregationsToPage() {
		//Given
		SearchHit[] hits = {createCarHit("Ford", "Grat"), createCarHit("BMW", "Arrow")};
		SearchHits searchHits = mock(SearchHits.class);
		when(searchHits.getTotalHits()).thenReturn(2L);
		when(searchHits.iterator()).thenReturn(new ArrayIterator(hits));
		when(response.getHits()).thenReturn(searchHits);

		Aggregations aggregations = new Aggregations(asList(createCarAggregation()));
		when(response.getAggregations()).thenReturn(aggregations);

		//When
		AggregatedPage<Car> page = (AggregatedPage<Car>) resultMapper.mapResults(response, Car.class, Pageable.unpaged());

		//Then
		page.hasFacets();
		assertThat(page.hasAggregations(), is(true));
		assertThat(page.getAggregation("Diesel").getName(), is("Diesel"));
	}

	@Test
	public void shouldMapSearchRequestToPage() {
		//Given
		SearchHit[] hits = {createCarHit("Ford", "Grat"), createCarHit("BMW", "Arrow")};
		SearchHits searchHits = mock(SearchHits.class);
		when(searchHits.getTotalHits()).thenReturn(2L);
		when(searchHits.iterator()).thenReturn(new ArrayIterator(hits));
		when(response.getHits()).thenReturn(searchHits);

		//When
		Page<Car> page = resultMapper.mapResults(response, Car.class, Pageable.unpaged());

		//Then
		assertThat(page.hasContent(), is(true));
		assertThat(page.getTotalElements(), is(2L));
		assertThat(page.getContent().get(0).getName(), is("Ford"));
	}

	@Test
	public void shouldMapPartialSearchRequestToObject() {
		//Given
		SearchHit[] hits = {createCarPartialHit("Ford", "Grat"), createCarPartialHit("BMW", "Arrow")};
		SearchHits searchHits = mock(SearchHits.class);
		when(searchHits.getTotalHits()).thenReturn(2L);
		when(searchHits.iterator()).thenReturn(new ArrayIterator(hits));
		when(response.getHits()).thenReturn(searchHits);

		//When
		Page<Car> page = resultMapper.mapResults(response, Car.class, Pageable.unpaged());

		//Then
		assertThat(page.hasContent(), is(true));
		assertThat(page.getTotalElements(), is(2L));
		assertThat(page.getContent().get(0).getName(), is("Ford"));
	}

	@Test
	public void shouldMapGetRequestToObject() {
		//Given
		GetResponse response = mock(GetResponse.class);
		when(response.getSourceAsString()).thenReturn(createJsonCar("Ford", "Grat"));

		//When
		Car result = resultMapper.mapResult(response, Car.class);

		//Then
		assertThat(result, notNullValue());
		assertThat(result.getModel(), is("Grat"));
		assertThat(result.getName(), is("Ford"));
	}

	/**
	 * @see DATAES-281.
	 */
	@Test
	@Ignore("fix me - UnsupportedOperation")
	public void setsIdentifierOnImmutableType() {

		GetResponse response = mock(GetResponse.class);
		when(response.getSourceAsString()).thenReturn("{}");
		when(response.getId()).thenReturn("identifier");

		ImmutableEntity result = resultMapper.mapResult(response, ImmutableEntity.class);

		assertThat(result, is(notNullValue()));
		assertThat(result.getId(), is("identifier"));
	}

	@Test // DATAES-198
	public void setsVersionFromGetResponse() {
		GetResponse response = mock(GetResponse.class);
		when(response.getSourceAsString()).thenReturn("{}");
		when(response.getVersion()).thenReturn(1234L);

		SampleEntity result = resultMapper.mapResult(response, SampleEntity.class);

		assertThat(result, is(notNullValue()));
		assertThat(result.getVersion(), is(1234L));
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

		LinkedList<SampleEntity> results = resultMapper.mapResults(multiResponse, SampleEntity.class);

		assertThat(results, is(notNullValue()));
		assertThat(results, hasSize(2));

		assertThat(results.get(0).getVersion(), is(1234L));
		assertThat(results.get(1).getVersion(), is(5678L));
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

		AggregatedPage<SampleEntity> results = resultMapper.mapResults(searchResponse, SampleEntity.class,
				mock(Pageable.class));

		assertThat(results, is(notNullValue()));

		assertThat(results.getContent().get(0).getVersion(), is(1234L));
		assertThat(results.getContent().get(1).getVersion(), is(5678L));
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
		final String q = "\"";
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
}
