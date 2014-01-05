package org.springframework.data.elasticsearch.core;

import org.apache.commons.collections.iterators.ArrayIterator;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.get.GetField;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.internal.InternalSearchHitField;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.data.elasticsearch.Car;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Artur Konczak
 */
public class DefaultResultMapperTests {

    private DefaultResultMapper resultMapper;

    @Mock
    private SearchResponse response;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        resultMapper = new DefaultResultMapper();
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
        FacetedPage<Car> page = resultMapper.mapResults(response, Car.class, null);

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
        FacetedPage<Car> page = resultMapper.mapResults(response, Car.class, null);

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

}
