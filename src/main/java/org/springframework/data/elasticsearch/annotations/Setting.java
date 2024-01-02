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
 * Elasticsearch Setting
 *
 * @author Mohsin Husen
 * @author Peter-Josef Meisch
 */

@Persistent
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface Setting {

	/**
	 * Resource path for a settings configuration
	 */
	String settingPath() default "";

	/**
	 * Use server-side settings when creating the index.
	 */
	boolean useServerConfiguration() default false;

	/**
	 * Number of shards for the index. Used for index creation. <br/>
	 * With version 4.0, the default value is changed from 5 to 1 to reflect the change in the default settings of
	 * Elasticsearch which changed to 1 as well in Elasticsearch 7.0.
	 */
	short shards() default 1;

	/**
	 * Number of replicas for the index. Used for index creation.
	 */
	short replicas() default 1;

	/**
	 * Refresh interval for the index. Used for index creation.
	 */
	String refreshInterval() default "1s";

	/**
	 * Index storage type for the index. Used for index creation.
	 */
	String indexStoreType() default "fs";

	/**
	 * fields to define an index sorting
	 *
	 * @since 4.2
	 */
	String[] sortFields() default {};

	/**
	 * defines the order for {@link #sortFields()}. If present, it must have the same number of elements
	 *
	 * @since 4.2
	 */
	SortOrder[] sortOrders() default {};

	/**
	 * defines the mode for {@link #sortFields()}. If present, it must have the same number of elements
	 *
	 * @since 4.2
	 */
	SortMode[] sortModes() default {};

	/**
	 * defines the missing value for {@link #sortFields()}. If present, it must have the same number of elements
	 *
	 * @since 4.2
	 */
	SortMissing[] sortMissingValues() default {};

	enum SortOrder {
		asc, desc
	}

	enum SortMode {
		min, max
	}

	enum SortMissing {
		_last, _first
	}
}
