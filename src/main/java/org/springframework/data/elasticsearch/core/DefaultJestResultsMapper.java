package org.springframework.data.elasticsearch.core;

import static org.apache.commons.lang.StringUtils.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.gson.JsonObject;
import io.searchbox.client.JestResult;
import io.searchbox.core.DocumentResult;
import io.searchbox.core.SearchResult;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.ScriptedField;
import org.springframework.data.elasticsearch.core.jest.MultiDocumentResult;
import org.springframework.data.elasticsearch.core.jest.SearchScrollResult;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.util.StringUtils;

/**
 * Jest implementation of Spring Data Elasticsearch results mapper.
 *
 * @author Julien Roy
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
	public <T> Page<T> mapResults(SearchScrollResult response, Class<T> clazz) {

		LinkedList<T> results = new LinkedList<T>();

		for (SearchScrollResult.Hit<JsonObject, Void> hit : response.getHits(JsonObject.class)) {
			if (hit != null) {
				results.add(mapSource(hit.source, clazz));
			}
		}

		return new PageImpl<T>(results, null, response.getTotal());
	}

	@Override
	public <T> Page<T> mapResults(SearchResult response, Class<T> clazz, Pageable pageable) {

		LinkedList<T> results = new LinkedList<T>();

		for (SearchResult.Hit<JsonObject, Void> hit : response.getHits(JsonObject.class)) {
			if (hit != null) {
				results.add(mapSource(hit.source, clazz));
			}
		}

		return new PageImpl<T>(results, pageable, response.getTotal());
	}

	private <T> T mapSource(JsonObject source, Class<T> clazz) {
		String sourceString = source.toString();
		T result = null;
		if (!StringUtils.isEmpty(sourceString)) {
			result = mapEntity(sourceString, clazz);
			setPersistentEntityId(result, source.get(JestResult.ES_METADATA_ID).getAsString(), clazz);
		} else {
			//TODO(Fields resutls) : Map Fields results
			//result = mapEntity(hit.getFields().values(), clazz);
		}
		return result;
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
}
