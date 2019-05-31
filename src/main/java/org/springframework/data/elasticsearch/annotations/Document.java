/*
 * Copyright 2013-2019 the original author or authors.
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

import org.elasticsearch.index.VersionType;

import org.springframework.data.annotation.Persistent;

/**
 * Identifies a domain object to be persisted to Elasticsearch.
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Mason Chan
 * @author Ivan Greene
 * @author Mark Paluch
 */
@Persistent
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface Document {

	/**
	 * Name of the Elasticsearch index.
	 * <ul>
	 * <li>Lowercase only</li>
	 * <li><Cannot include \, /, *, ?, ", <, >, |, ` ` (space character), ,, #/li>
	 * <li>Cannot start with -, _, +</li>
	 * <li>Cannot be . or ..</li>
	 * <li>Cannot be longer than 255 bytes (note it is bytes, so multi-byte characters will count towards the 255 limit
	 * faster)</li>
	 * </ul>
	 */
	String indexName();

	/**
	 * Mapping type name.
	 */
	String type() default "";

	/**
	 * Use server-side settings when creating the index.
	 */
	boolean useServerConfiguration() default false;

	/**
	 * Number of shards for the index {@link #indexName()}. Used for index creation.
	 */
	short shards() default 5;

	/**
	 * Number of replicas for the index {@link #indexName()}. Used for index creation.
	 */
	short replicas() default 1;

	/**
	 * Refresh interval for the index {@link #indexName()}. Used for index creation.
	 */
	String refreshInterval() default "1s";

	/**
	 * Index storage type for the index {@link #indexName()}. Used for index creation.
	 */
	String indexStoreType() default "fs";

	/**
	 * Configuration whether to create an index on repository bootstrapping.
	 */
	boolean createIndex() default true;

	/**
	 * Configuration of version management.
	 */
	VersionType versionType() default VersionType.EXTERNAL;
}
