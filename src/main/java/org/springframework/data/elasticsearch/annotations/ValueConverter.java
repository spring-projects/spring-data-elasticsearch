/*
 * Copyright 2021-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.data.elasticsearch.core.mapping.PropertyValueConverter;

/**
 * Annotation to put on a property of an entity to define a value converter which can convert the property to a type
 * that Elasticsearch understands and back.
 *
 * @author Peter-Josef Meisch
 * @since 4.3
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Documented
@Inherited
public @interface ValueConverter {

	/**
	 * Defines the class implementing the {@link PropertyValueConverter} interface. If this is a normal class, it must
	 * provide a default constructor with no arguments. If this is an enum and thus implementing a singleton by enum it
	 * must only have one enum value.
	 * 
	 * @return the class to use for conversion
	 */
	Class<? extends PropertyValueConverter> value();
}
