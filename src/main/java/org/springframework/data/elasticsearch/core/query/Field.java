/*
 * Copyright 2013-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.core.query;

import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.lang.Nullable;

/**
 * Defines a Field that can be used within a Criteria.
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Peter-Josef Meisch
 */
public interface Field {

	void setName(String name);

	String getName();

	/**
	 * @param fieldType sets the field's type
	 */
	void setFieldType(FieldType fieldType);

	/**
	 * @return The annotated FieldType of the field
	 */
	@Nullable
	FieldType getFieldType();

	/**
	 * Sets the path if this field has a multi-part name that should be used in a nested query.
	 *
	 * @param path the value to set
	 * @since 4.2
	 */
	void setPath(@Nullable String path);

	/**
	 * @return the path if this is a field for a nested query
	 * @since 4.2
	 */
	@Nullable
	String getPath();
}
