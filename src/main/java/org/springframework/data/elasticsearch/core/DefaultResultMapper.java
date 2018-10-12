/*
 * Copyright 2014-2018 the original author or authors.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.document.DocumentField;
import org.elasticsearch.search.SearchHit;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.ScriptedField;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.ConvertingPropertyAccessor;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.springframework.util.StringUtils;

/**
 * @author Artur Konczak
 * @author Petar Tahchiev
 * @author Young Gu
 * @author Oliver Gierke
 * @author Chris White
 * @author Mark Paluch
 * @author Ilkang Na
 * @author Sascha Woo
 */
public class DefaultResultMapper extends AbstractResultMapper {

	private final MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext;

	private final ConversionService conversionService = new DefaultConversionService();

	public DefaultResultMapper() {
		this(new SimpleElasticsearchMappingContext());
	}

	public DefaultResultMapper(MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext) {
		
		super(new DefaultEntityMapper(mappingContext));
		
		Assert.notNull(mappingContext, "MappingContext must not be null!");
		
		this.mappingContext = mappingContext;
	}

	public DefaultResultMapper(EntityMapper entityMapper) {
		this(new SimpleElasticsearchMappingContext(), entityMapper);
	}

	public DefaultResultMapper(
			MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext,
			EntityMapper entityMapper) {
		
		super(entityMapper);
		
		Assert.notNull(mappingContext, "MappingContext must not be null!");
		
		this.mappingContext = mappingContext;
	}

	@Override
	public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
		
		long totalHits = response.getHits().getTotalHits();
		float maxScore = response.getHits().getMaxScore();

		List<T> results = new ArrayList<>();
		for (SearchHit hit : response.getHits()) {
			if (hit != null) {
				T result = null;
				if (!StringUtils.isEmpty(hit.getSourceAsString())) {
					result = mapEntity(hit.getSourceAsString(), clazz);
				} else {
					result = mapEntity(hit.getFields().values(), clazz);
				}

				setPersistentEntityId(result, hit.getId(), clazz);
				setPersistentEntityVersion(result, hit.getVersion(), clazz);
				setPersistentEntityScore(result, hit.getScore(), clazz);
				
				populateScriptFields(result, hit);
				results.add(result);
			}
		}

		return new AggregatedPageImpl<T>(results, pageable, totalHits, response.getAggregations(), response.getScrollId(),
				maxScore);
	}

	private <T> void populateScriptFields(T result, SearchHit hit) {
		if (hit.getFields() != null && !hit.getFields().isEmpty() && result != null) {
			for (java.lang.reflect.Field field : result.getClass().getDeclaredFields()) {
				ScriptedField scriptedField = field.getAnnotation(ScriptedField.class);
				if (scriptedField != null) {
					String name = scriptedField.name().isEmpty() ? field.getName() : scriptedField.name();
					DocumentField searchHitField = hit.getFields().get(name);
					if (searchHitField != null) {
						field.setAccessible(true);
						try {
							field.set(result, searchHitField.getValue());
						} catch (IllegalArgumentException e) {
							throw new ElasticsearchException(
									"failed to set scripted field: " + name + " with value: " + searchHitField.getValue(), e);
						} catch (IllegalAccessException e) {
							throw new ElasticsearchException("failed to access scripted field: " + name, e);
						}
					}
				}
			}
		}
	}

	private <T> T mapEntity(Collection<DocumentField> values, Class<T> clazz) {
		return mapEntity(buildJSONFromFields(values), clazz);
	}

	private String buildJSONFromFields(Collection<DocumentField> values) {
		JsonFactory nodeFactory = new JsonFactory();
		try {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			JsonGenerator generator = nodeFactory.createGenerator(stream, JsonEncoding.UTF8);
			generator.writeStartObject();
			for (DocumentField value : values) {
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

	@Override
	public <T> T mapResult(GetResponse response, Class<T> clazz) {
		T result = mapEntity(response.getSourceAsString(), clazz);
		if (result != null) {
			setPersistentEntityId(result, response.getId(), clazz);
			setPersistentEntityVersion(result, response.getVersion(), clazz);
		}
		return result;
	}

	@Override
	public <T> LinkedList<T> mapResults(MultiGetResponse responses, Class<T> clazz) {
		LinkedList<T> list = new LinkedList<>();
		for (MultiGetItemResponse response : responses.getResponses()) {
			if (!response.isFailed() && response.getResponse().isExists()) {
				T result = mapEntity(response.getResponse().getSourceAsString(), clazz);
				setPersistentEntityId(result, response.getResponse().getId(), clazz);
				setPersistentEntityVersion(result, response.getResponse().getVersion(), clazz);
				list.add(result);
			}
		}
		return list;
	}

	private <T> void setPersistentEntityId(T result, String id, Class<T> clazz) {
		
		if (clazz.isAnnotationPresent(Document.class)) {
			
			ElasticsearchPersistentEntity<?> persistentEntity = mappingContext.getRequiredPersistentEntity(clazz);
			ElasticsearchPersistentProperty idProperty = persistentEntity.getIdProperty();

			PersistentPropertyAccessor<T> accessor = new ConvertingPropertyAccessor<>(persistentEntity.getPropertyAccessor(result),
					conversionService);

			// Only deal with String because ES generated Ids are strings !
			if (idProperty != null && idProperty.getType().isAssignableFrom(String.class)) {
				accessor.setProperty(idProperty, id);
			}
		}
	}

	private <T> void setPersistentEntityVersion(T result, long version, Class<T> clazz) {
		
		if (clazz.isAnnotationPresent(Document.class)) {
			
			ElasticsearchPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(clazz);
			ElasticsearchPersistentProperty versionProperty = persistentEntity.getVersionProperty();

			// Only deal with Long because ES versions are longs !
			if (versionProperty != null && versionProperty.getType().isAssignableFrom(Long.class)) {
				// check that a version was actually returned in the response, -1 would indicate that
				// a search didn't request the version ids in the response, which would be an issue
				Assert.isTrue(version != -1, "Version in response is -1");
				persistentEntity.getPropertyAccessor(result).setProperty(versionProperty, version);
			}
		}
	}

	private <T> void setPersistentEntityScore(T result, float score, Class<T> clazz) {

		if (clazz.isAnnotationPresent(Document.class)) {

			ElasticsearchPersistentEntity<?> entity = mappingContext.getRequiredPersistentEntity(clazz);

			if (!entity.hasScoreProperty()) {
				return;
			}

			entity.getPropertyAccessor(result) //
					.setProperty(entity.getScoreProperty(), score);
		}
	}
}
