/*
 * Copyright 2013-2014 the original author or authors.
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
import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.data.elasticsearch.core.partition.DefaultElasticsearchPartitioner;
import org.springframework.data.elasticsearch.core.partition.ElasticsearchPartitioner;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.util.TypeInformation;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.springframework.util.StringUtils.hasText;

/**
 * Elasticsearch specific {@link org.springframework.data.mapping.PersistentEntity} implementation holding
 *
 * @param <T>
 * @author Rizwan Idrees
 * @author Mohsin Husen
 */
public class SimpleElasticsearchPersistentEntity<T> extends BasicPersistentEntity<T, ElasticsearchPersistentProperty>
		implements ElasticsearchPersistentEntity<T>, ApplicationContextAware {

	private final StandardEvaluationContext context;
	private final SpelExpressionParser parser;

	private String indexName;
	private String indexType;
	private short shards;
	private short replicas;
	private String refreshInterval;
	private String indexStoreType;
	private String parentType;
	private ElasticsearchPersistentProperty parentIdProperty;
	private String settingPath;
	private String[] partitionersFields;
	private Partitioner[] partitioners;
	private String[] partitionersParameters;
	private String partitionSeparator;
	private ElasticsearchPartitioner indexPartitioner;
	Map<String, ElasticsearchPersistentProperty> innerHitsProperties;


	public SimpleElasticsearchPersistentEntity(TypeInformation<T> typeInformation) {
		super(typeInformation);
		this.context = new StandardEvaluationContext();
		this.parser = new SpelExpressionParser();
		this.indexPartitioner = new DefaultElasticsearchPartitioner();

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
			this.partitionersFields = typeInformation.getType().getAnnotation(Document.class).partitionersFields();
			this.partitioners = typeInformation.getType().getAnnotation(Document.class).partitioners();
			this.partitionersParameters = typeInformation.getType().getAnnotation(Document.class).partitionersParameters();
			this.partitionSeparator = typeInformation.getType().getAnnotation(Document.class).partitionSeparator();
		}
		if (clazz.isAnnotationPresent(Setting.class)) {
			this.settingPath = typeInformation.getType().getAnnotation(Setting.class).settingPath();
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		context.addPropertyAccessor(new BeanFactoryAccessor());
		context.setBeanResolver(new BeanFactoryResolver(applicationContext));
		context.setRootObject(applicationContext);
	}

	public Partitioner[] getPartitioners() {
		return partitioners;
	}

	@Override
	public String[] getPartitionersParameters() {
		String[] parameters = new String[partitionersParameters.length];
		for (int i = 0; i < partitionersParameters.length; i++) {
			Expression expression = parser.parseExpression(partitionersParameters[i], ParserContext.TEMPLATE_EXPRESSION);
			parameters[i] = expression.getValue(context, String.class);
		}
		return parameters;
	}

	@Override
	public String[] getPartitionersFields() {
		return partitionersFields;
	}

	@Override
	public String getPartitionSeparator() {
		return partitionSeparator;
	}

	@Override
	public String getIndexName() {
		Expression expression = parser.parseExpression(indexName, ParserContext.TEMPLATE_EXPRESSION);
		return expression.getValue(context, String.class);
	}

	@Override
	public String getIndexName(T object) {
		Expression expression = parser.parseExpression(indexName, ParserContext.TEMPLATE_EXPRESSION);
		String indexName = expression.getValue(context, String.class);
		if (partitioners.length == 0) return indexName;
		String partitionPostfix = indexPartitioner.extractPartitionKeyFromObject(object, this);
		return indexName+partitionSeparator+partitionPostfix;
	}


	@Override
	public String getIndexType() {
		Expression expression = parser.parseExpression(indexType, ParserContext.TEMPLATE_EXPRESSION);
		return expression.getValue(context, String.class);
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
	public String settingPath() {
		Expression expression = parser.parseExpression(settingPath, ParserContext.TEMPLATE_EXPRESSION);
		return expression.getValue(context, String.class);
	}

	@Override
	public Map<String, ElasticsearchPersistentProperty> innerHitsProperties() {
		return innerHitsProperties;
	}

	@Override
	public void addPersistentProperty(ElasticsearchPersistentProperty property) {
		super.addPersistentProperty(property);

		if (property.getField() != null) {
			Parent parent = property.getField().getAnnotation(Parent.class);
			if (parent != null) {
				Assert.isNull(this.parentIdProperty, "Only one field can hold a @Parent annotation");
				Assert.isNull(this.parentType, "Only one field can hold a @Parent annotation");
				Assert.isTrue(property.getType() == String.class, "Parent ID property should be String");
				this.parentIdProperty = property;
				this.parentType = parent.type();
			}
		}

		if (property.isVersionProperty()) {
			Assert.isTrue(property.getType() == Long.class, "Version property should be Long");
		}

		InnerHits innerHits = property.getField().getAnnotation(InnerHits.class);
		if (innerHits != null) {
			if (innerHitsProperties == null) innerHitsProperties = new HashMap<String, ElasticsearchPersistentProperty>();
			Assert.isTrue(!innerHitsProperties.containsKey(innerHits.path()), "Only one filed can be mapped with the same innerHi path");
			innerHitsProperties.put(innerHits.path(), property);
		}
	}
}
