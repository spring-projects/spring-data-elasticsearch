/*
 * Copyright 2022-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.core.script;

import org.springframework.lang.Nullable;

/**
 * This interfaces defines the operations to access the
 * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/8.5/script-apis.html">Elasticsearch script API</a>.
 *
 * @author Peter-Josef Meisch
 * @since 5.1
 */
public interface ScriptOperations {
	/**
	 * Stores the given script in the Elasticsearch cluster.
	 *
	 * @return {{@literal true} if successful
	 */
	boolean putScript(Script script);

	/**
	 * Gest the script with the given name.
	 *
	 * @param name the name of the script
	 * @return Script or null when a script with this name does not exist.
	 */
	@Nullable
	Script getScript(String name);

	/**
	 * Deletes the script with the given name
	 *
	 * @param name the name of the script.
	 * @return true if the request was acknowledged by the cluster.
	 */
	boolean deleteScript(String name);
}
