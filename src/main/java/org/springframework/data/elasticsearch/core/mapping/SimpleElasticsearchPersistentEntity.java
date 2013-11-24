/*
 * Copyright 2013 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.expression.BeanFactoryAccessor;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Parent;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.util.TypeInformation;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.Assert;

import java.util.Locale;

import static org.springframework.util.StringUtils.hasText;

/**
 * Elasticsearch specific {@link org.springframework.data.mapping.PersistentEntity} implementation holding
 * 
 * @param <T>
 * 
 * @author Rizwan Idrees
 * @author Mohsin Husen
 */
public class SimpleElasticsearchPersistentEntity<T> extends BasicPersistentEntity<T, ElasticsearchPersistentProperty>
		implements ElasticsearchPersistentEntity<T>, ApplicationContextAware {

	private final StandardEvaluationContext context;
	private String indexName;
	private String indexType;
	private short shards;
	private short replicas;
	private String refreshInterval;
	private String indexStoreType;
	private String parentType;
	private ElasticsearchPersistentProperty parentIdProperty;

	public SimpleElasticsearchPersistentEntity(TypeInformation<T> typeInformation) {
		super(typeInformation);
		this.context = new StandardEvaluationContext();
		Class<T> clazz = typeInformation.getType();
		if (clazz.isAnnotationPresent(Document.class)) {
			Document document = clazz.getAnnotation(Document.class);
			Assert.hasText(document.indexName(),
					" Unknown indexName. Make sure the indexName is defined. e.g @Document(indexName=\"foo\")");
			this.indexName = typeInformation.getType().getAnnotation(Document.class).indexName();
			this.indexType = hasText(document.type()) ? document.type() : clazz.getSimpleName().toLowerCase(Locale.ENGLISH);
			this.shards = typeInformation.getType().getAnnotation(Document.class).shards();
			this.replicas = typeInformation.getType().getAnnotation(Document.class).replicas();
			this.refreshInterval = typeInformation.getType().getAnnotation(Document.class).refreshInterval();
			this.indexStoreType = typeInformation.getType().getAnnotation(Document.class).indexStoreType();
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		context.addPropertyAccessor(new BeanFactoryAccessor());
		context.setBeanResolver(new BeanFactoryResolver(applicationContext));
		context.setRootObject(applicationContext);
	}

	@Override
	public String getIndexName() {
		return indexName;
	}

	@Override
	public String getIndexType() {
		return indexType;
	}

	@Override
	public String getIndexStoreType() {
		return indexStoreType;
	}

	@Override
	public short getShards() {
		return shards;
	}

	@Override
	public short getReplicas() {
		return replicas;
	}

	@Override
	public String getRefreshInterval() {
		return refreshInterval;
	}

	@Override
	public String getParentType() {
		return parentType;
	}

	@Override
	public ElasticsearchPersistentProperty getParentIdProperty() {
		return parentIdProperty;
	}

	@Override
	public void addPersistentProperty(ElasticsearchPersistentProperty property) {
		super.addPersistentProperty(property);
		
		Parent parent = property.getField().getAnnotation(Parent.class);
		if (parent !=null) {
			Assert.isNull(this.parentIdProperty, "Only one field can hold a @Parent annotation");
			Assert.isNull(this.parentType, "Only one field can hold a @Parent annotation");
			Assert.isTrue(property.getType() == String.class, "Parent ID property should be String");
			this.parentIdProperty = property;
			this.parentType = parent.type();
		}
		
		if (property.isVersionProperty()) {
			Assert.isTrue(property.getType() == Long.class, "Version property should be Long");
		}
	}
}
