/*
 * Copyright 2014-2024 the original author or authors.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.data.annotation.Persistent;

/**
 * Elasticsearch Mapping
 *
 * @author Mohsin Husen
 * @author Peter-Josef Meisch
 */
@Persistent
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD })
public @interface Mapping {

	String mappingPath() default "";

	/**
	 * whether mappings are enabled
	 *
	 * @since 4.2
	 */
	boolean enabled() default true;

	/**
	 * whether date_detection is enabled
	 *
	 * @since 4.3
	 */
	Detection dateDetection() default Detection.DEFAULT;

	/**
	 * whether numeric_detection is enabled
	 *
	 * @since 4.3
	 */
	Detection numericDetection() default Detection.DEFAULT;

	/**
	 * custom dynamic date formats
	 *
	 * @since 4.3
	 */
	String[] dynamicDateFormats() default {};

	/**
	 * classpath to a JSON file containing the values for a runtime mapping definition. The file must contain the JSON
	 * object that is written as the value of the runtime property. {@see <a href=
	 * "https://www.elastic.co/guide/en/elasticsearch/reference/7.12/runtime-mapping-fields.html">elasticsearch doc</a>}
	 *
	 * @since 4.3
	 */
	String runtimeFieldsPath() default "";

	/**
	 * field alias definitions to be written to the index mapping
	 *
	 * @since 5.3
	 */
	MappingAlias[] aliases() default {};

	enum Detection {
		DEFAULT, TRUE, FALSE
	}
}
