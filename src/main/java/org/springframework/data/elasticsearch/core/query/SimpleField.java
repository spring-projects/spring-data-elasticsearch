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
import org.springframework.util.Assert;

import java.util.Objects;

/**
 * The most trivial implementation of a Field. The {@link #name} is updatable, so it may be changed during query
 * preparation by the {@link org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter}.
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Peter-Josef Meisch
 */
public class SimpleField implements Field {

	private String name;
	@Nullable private FieldType fieldType;
	@Nullable private String path;

	public SimpleField(String name) {

		Assert.hasText(name, "name must not be null");

		this.name = name;
	}

	@Override
	public void setName(String name) {

		Assert.hasText(name, "name must not be null");

		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setFieldType(FieldType fieldType) {
		this.fieldType = fieldType;
	}

	@Nullable
	@Override
	public FieldType getFieldType() {
		return fieldType;
	}

	@Override
	public void setPath(@Nullable String path) {
		this.path = path;
	}

	@Override
	@Nullable
	public String getPath() {
		return path;
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof SimpleField that)) return false;
        return Objects.equals(name, that.name) && Objects.equals(fieldType, that.fieldType) && Objects.equals(path, that.path);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, fieldType, path);
	}
}
