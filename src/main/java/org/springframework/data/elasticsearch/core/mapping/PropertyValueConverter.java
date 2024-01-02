/*
 * Copyright 2019-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.core.mapping;

/**
 * Interface defining methods to convert the value of an entity-property to a value in Elasticsearch and back.
 *
 * @author Peter-Josef Meisch
 * @author Sascha Woo
 */
public interface PropertyValueConverter {

	/**
	 * Converts a property value to an elasticsearch value. If the converter cannot convert the value, it must return a
	 * String representation.
	 *
	 * @param value the value to convert, must not be {@literal null}
	 * @return The elasticsearch property value, must not be {@literal null}
	 */
	Object write(Object value);

	/**
	 * Converts an elasticsearch property value to a property value.
	 *
	 * @param value the elasticsearch property value to convert, must not be {@literal null}
	 * @return The converted value, must not be {@literal null}
	 */
	Object read(Object value);
}
