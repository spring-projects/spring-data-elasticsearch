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
package org.springframework.data.elasticsearch.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.data.annotation.Persistent;

/**
 * Identifies a domain object to be persisted to Elasticsearch.
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Mason Chan
 * @author Ivan Greene
 * @author Mark Paluch
 * @author Peter-Josef Meisch
 * @author Sascha Woo
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
	 * <li>Cannot include \, /, *, ?, ", &gt;, &lt;, |, ` ` (space character), ,, #</li>
	 * <li>Cannot start with -, _, +</li>
	 * <li>Cannot be . or ..</li>
	 * <li>Cannot be longer than 255 bytes (note it is bytes, so multi-byte characters will count towards the 255 limit
	 * faster)</li>
	 * </ul>
	 */
	String indexName();

	/**
	 * Configuration whether to create an index on repository bootstrapping.
	 */
	boolean createIndex() default true;

	/**
	 * If true, the index mapping will be written on repository bootstrapping even when the index already exists. This
	 * allows for automatically updating the mapping with new properties. Changes on existing properties will lead to an
	 * error from the Elasticsearch server.
	 */
	boolean alwaysWriteMapping() default false;

	/**
	 * Configuration of version management.
	 */
	VersionType versionType() default VersionType.EXTERNAL;

	/**
	 * Defines if type hints should be written. {@see WriteTypeHint}.
	 *
	 * @since 4.3
	 */
	WriteTypeHint writeTypeHint() default WriteTypeHint.DEFAULT;

	/**
	 * Controls how Elasticsearch dynamically adds fields to the document.
	 *
	 * @since 4.3
	 */
	Dynamic dynamic() default Dynamic.INHERIT;

	/**
	 * Specifies if the id property should also be stored in the Elasticsearch document source. Default value is
	 * {@literal true}
	 *
	 * @since 5.1
	 */
	boolean storeIdInSource() default true;

	/**
	 * Specifies if the version property should also be stored in the Elasticsearch document source. Default value is
	 * true.
	 *
	 * @since 5.1
	 */
	boolean storeVersionInSource() default true;

	/**
	 * Aliases for the index.
	 *
	 * @since 5.4
	 */
	Alias[] aliases() default {};

	/**
	 * @since 4.3
	 */
	enum VersionType {
		INTERNAL("internal"), //
		EXTERNAL("external"), //
		EXTERNAL_GTE("external_gte"), //
		/**
		 * @since 4.4
		 */
		FORCE("force");

		private final String esName;

		VersionType(String esName) {
			this.esName = esName;
		}

		public String getEsName() {
			return esName;
		}
	}
}
