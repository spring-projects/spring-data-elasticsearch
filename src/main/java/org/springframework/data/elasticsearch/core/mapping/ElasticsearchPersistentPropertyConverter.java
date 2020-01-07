/*
 * Copyright 2019-2020 the original author or authors.
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
 * Interface defining methods to convert a property value to a String and back.
 *
 * @author Peter-Josef Meisch
 */
public interface ElasticsearchPersistentPropertyConverter {

	/**
	 * converts the property value to a String.
	 *
	 * @param property the property value to convert, must not be {@literal null}
	 * @return String representation.
	 */
	String write(Object property);

	/**
	 * converts a property value from a String.
	 *
	 * @param s the property to convert, must not be {@literal null}
	 * @return property value
	 */
	Object read(String s);
}
