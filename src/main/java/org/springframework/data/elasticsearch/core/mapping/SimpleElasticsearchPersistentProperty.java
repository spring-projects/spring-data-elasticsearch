/*
 * Copyright 2013-2018 the original author or authors.
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
package org.springframework.data.elasticsearch.core.mapping;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.elasticsearch.annotations.Score;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;

/**
 * Elasticsearch specific {@link org.springframework.data.mapping.PersistentProperty} implementation processing
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Mark Paluch
 * @author Sascha Woo
 * @author Oliver Gierke
 */
public class SimpleElasticsearchPersistentProperty extends
		AnnotationBasedPersistentProperty<ElasticsearchPersistentProperty> implements ElasticsearchPersistentProperty {

	private static final Set<Class<?>> SUPPORTED_ID_TYPES = new HashSet<>();
	private static final Set<String> SUPPORTED_ID_PROPERTY_NAMES = new HashSet<>();
	
	private final boolean isScore; 

	static {
		SUPPORTED_ID_TYPES.add(String.class);
		SUPPORTED_ID_PROPERTY_NAMES.add("id");
		SUPPORTED_ID_PROPERTY_NAMES.add("documentId");
	}

	public SimpleElasticsearchPersistentProperty(Property property,
			PersistentEntity<?, ElasticsearchPersistentProperty> owner, SimpleTypeHolder simpleTypeHolder) {
		
		super(property, owner, simpleTypeHolder);
		
		this.isScore = isAnnotationPresent(Score.class);
		
		if (isScore && !Arrays.asList(Float.TYPE, Float.class).contains(getType())) {
			throw new MappingException(String.format("Score property %s must be either of type float or Float!", property.getName()));
		}
	}

	@Override
	public String getFieldName() {
		return getProperty().getName();
	}

	@Override
	public boolean isIdProperty() {
		return super.isIdProperty() || SUPPORTED_ID_PROPERTY_NAMES.contains(getFieldName());
	}

	@Override
	protected Association<ElasticsearchPersistentProperty> createAssociation() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty#isScoreProperty()
	 */
	@Override
	public boolean isScoreProperty() {
		return isScore;
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.AbstractPersistentProperty#isImmutable()
	 */
	@Override
	public boolean isImmutable() {
		return false;
	}
}
