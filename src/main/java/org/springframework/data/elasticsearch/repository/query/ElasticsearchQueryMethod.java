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
package org.springframework.data.elasticsearch.repository.query;

import java.lang.reflect.Method;

import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.elasticsearch.annotations.Highlight;
import org.springframework.data.elasticsearch.annotations.HighlightField;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;

/**
 * ElasticsearchQueryMethod
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public class ElasticsearchQueryMethod extends QueryMethod {

	private final Query queryAnnotation;
	private final Highlight highlightAnnotation;

	public ElasticsearchQueryMethod(Method method, RepositoryMetadata metadata, ProjectionFactory factory) {
		super(method, metadata, factory);
		this.queryAnnotation = method.getAnnotation(Query.class);
		this.highlightAnnotation = method.getAnnotation(Highlight.class);
	}

	public boolean hasAnnotatedQuery() {
		return this.queryAnnotation != null;
	}

	public String getAnnotatedQuery() {
		return (String) AnnotationUtils.getValue(queryAnnotation, "value");
	}

	public boolean hasHighlight() {
		return highlightAnnotation != null;
	}

	public HighlightBuilder getHighlight() {
		HighlightBuilder highlightBuilder = new HighlightBuilder();
		if (hasHighlight()) {
			for (HighlightField field : highlightAnnotation.fields()) {
				highlightBuilder.field(new HighlightBuilder.Field(field.name()).fragmentOffset(field.fragmentOffset())
						.fragmentSize(field.fragmentSize()).preTags(field.preTags()).postTags(field.postTags()));
			}
		}
		return highlightBuilder;
	}

	public boolean hasExtras() {
		return hasHighlight();
	}

	public NativeSearchQueryBuilder toNativeSearchBuilder() {
		return new NativeSearchQueryBuilder().withHighlightFields(getHighlight().fields().toArray(new HighlightBuilder.Field[0]));
	}
}
