/*
 * Copyright 2013-2016 the original author or authors.
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.internal.InternalSearchHitField;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.entities.Car;

import com.fasterxml.jackson.databind.util.ArrayIterator;

/**
 * @author Artur Konczak
 * @author Mohsin Husen
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
	public void shouldMapSearchRequestToPage() {
		//Given
		SearchHit[] hits = {createCarHit("Ford", "Grat"), createCarHit("BMW", "Arrow")};
		SearchHits searchHits = mock(SearchHits.class);
		when(searchHits.totalHits()).thenReturn(2L);
		when(searchHits.iterator()).thenReturn(new ArrayIterator(hits));
		when(response.getHits()).thenReturn(searchHits);

		//When
		Page<Car> page = resultMapper.mapResults(response, Car.class, null);

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
		when(searchHits.totalHits()).thenReturn(2L);
		when(searchHits.iterator()).thenReturn(new ArrayIterator(hits));
		when(response.getHits()).thenReturn(searchHits);

		//When
		Page<Car> page = resultMapper.mapResults(response, Car.class, null);

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
	public void setsIdentifierOnImmutableType() {
		
		GetResponse response = mock(GetResponse.class);
		when(response.getSourceAsString()).thenReturn("{}");
		when(response.getId()).thenReturn("identifier");
		
		ImmutableEntity result = resultMapper.mapResult(response, ImmutableEntity.class);
		
		assertThat(result, is(notNullValue()));
		assertThat(result.getId(), is("identifier"));
	}

	private SearchHit createCarHit(String name, String model) {
		SearchHit hit = mock(SearchHit.class);
		when(hit.sourceAsString()).thenReturn(createJsonCar(name, model));
		return hit;
	}

	private SearchHit createCarPartialHit(String name, String model) {
		SearchHit hit = mock(SearchHit.class);
		when(hit.sourceAsString()).thenReturn(null);
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

	private Map<String, SearchHitField> createCarFields(String name, String model) {
		Map<String, SearchHitField> result = new HashMap<String, SearchHitField>();
		result.put("name", new InternalSearchHitField("name", Arrays.<Object>asList(name)));
		result.put("model", new InternalSearchHitField("model", Arrays.<Object>asList(model)));
		return result;
	}
	
	@Document(indexName = "someIndex")
	@NoArgsConstructor(force = true)
	@Getter
	static class ImmutableEntity {

		private final String id, name;
	}
}
