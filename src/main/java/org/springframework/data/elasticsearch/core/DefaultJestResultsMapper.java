package org.springframework.data.elasticsearch.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.search.SearchHitField;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.jest.MultiDocumentResult;
import org.springframework.data.elasticsearch.core.jest.ScrollSearchResult;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.MappingContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import io.searchbox.client.JestResult;
import io.searchbox.core.DocumentResult;
import io.searchbox.core.SearchResult;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * Jest implementation of Spring Data Elasticsearch results mapper.
 *
 * @author Julien Roy
 * @author Robert Gruendler
 */
public class DefaultJestResultsMapper implements JestResultsMapper {

	private EntityMapper entityMapper;
	private MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext;

	public DefaultJestResultsMapper() {
		this.entityMapper = new DefaultEntityMapper();
	}

	public DefaultJestResultsMapper(EntityMapper entityMapper) {
		this.entityMapper = entityMapper;
	}

	public DefaultJestResultsMapper(MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext) {
		this.entityMapper = new DefaultEntityMapper();
		this.mappingContext = mappingContext;
	}

	public DefaultJestResultsMapper(MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext, EntityMapper entityMapper) {
		this.entityMapper = entityMapper;
		this.mappingContext = mappingContext;
	}

	@Override
	public EntityMapper getEntityMapper() {
		return this.entityMapper;
	}

	@Override
	public <T> T mapResult(DocumentResult response, Class<T> clazz) {
		T result = mapEntity(response.getJsonObject().get("_source").toString(), clazz);
		if (result != null) {
			setPersistentEntityId(result, response.getId(), clazz);
		}
		return result;
	}

	@Override
    public <T> LinkedList<T> mapResults(MultiDocumentResult multiResponse, Class<T> clazz) {

        LinkedList<T> results = new LinkedList<T>();

        for (MultiDocumentResult.MultiDocumentResultItem item : multiResponse.getItems()) {
            T result = mapEntity(item.getSource(), clazz);
            setPersistentEntityId(result, item.getId(), clazz);
            results.add(result);
        }

        return results;
    }

	@Override
	public <T> Page<T> mapResults(ScrollSearchResult response, Class<T> clazz) {
		List<T> results = new ArrayList<T>();

		for(JsonElement elem : response.getJsonObject().get("hits").getAsJsonObject().get("hits").getAsJsonArray()) {
			JsonObject hit = elem.getAsJsonObject();

			if (hit.get("_source") != null) {
				T result = mapEntity(hit.get("_source").toString(), clazz);
				setPersistentEntityId(result, hit.get("_id").getAsString(), clazz);
				results.add(result);
			} else {
				//TODO(Fields resutls) : Map Fields results
				//result = mapEntity(hit.getAsJsonObject().get("fields").getAsJsonObject(), clazz);
			}
		}

		//TODO: Add Total And Pageable
		return new PageImpl<T>(results, null, 0);
	}

	private <T> T mapEntity(Collection<SearchHitField> values, Class<T> clazz) {
		return mapEntity(buildJSONFromFields(values), clazz);
	}

	private <T> T mapEntity(String source, Class<T> clazz) {
		if (isBlank(source)) {
			return null;
		}
		try {
			return entityMapper.mapToObject(source, clazz);
		} catch (IOException e) {
			throw new ElasticsearchException("failed to map source [ " + source + "] to class " + clazz.getSimpleName(), e);
		}
	}

	private String buildJSONFromFields(Collection<SearchHitField> values) {
		JsonFactory nodeFactory = new JsonFactory();
		try {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			JsonGenerator generator = nodeFactory.createGenerator(stream, JsonEncoding.UTF8);
			generator.writeStartObject();
			for (SearchHitField value : values) {
				if (value.getValues().size() > 1) {
					generator.writeArrayFieldStart(value.getName());
					for (Object val : value.getValues()) {
						generator.writeObject(val);
					}
					generator.writeEndArray();
				} else {
					generator.writeObjectField(value.getName(), value.getValue());
				}
			}
			generator.writeEndObject();
			generator.flush();
			return new String(stream.toByteArray(), Charset.forName("UTF-8"));
		} catch (IOException e) {
			return null;
		}
	}

	private <T> void setPersistentEntityId(T result, String id, Class<T> clazz) {
		if (mappingContext != null && clazz.isAnnotationPresent(Document.class)) {
			PersistentProperty<ElasticsearchPersistentProperty> idProperty = mappingContext.getPersistentEntity(clazz).getIdProperty();
			// Only deal with String because ES generated Ids are strings !
			if (idProperty != null && idProperty.getType().isAssignableFrom(String.class)) {
				Method setter = idProperty.getSetter();
				if (setter != null) {
					try {
						setter.invoke(result, id);
					} catch (Throwable t) {
						t.printStackTrace();
					}
				}
			}
		}
	}

	@Override
	public <T> Page<T> mapResults(SearchResult response, Class<T> clazz, Pageable pageable, SearchQuery query) {

		List<T> results = new ArrayList<T>();
		for (SearchResult.Hit<JsonObject, Void> hit : response.getHits(JsonObject.class)) {
			if (hit != null) {
				T result = null;
				if (!StringUtils.isEmpty(hit.source.toString())) {
					result = mapEntity(hit.source.toString(), clazz);
					// http://stackoverflow.com/a/16640647/965338
					setPersistentEntityId(result, hit.source.get(JestResult.ES_METADATA_ID).getAsString(), clazz);
				} else {
					//TODO(Fields results) : Map Fields results
					//result = mapEntity(hit.getAsJsonObject().get("fields").getAsJsonObject(), clazz);
				}
				results.add(result);
			}
		}

		return new PageImpl<T>(results, pageable, response.getTotal());
	}
}
