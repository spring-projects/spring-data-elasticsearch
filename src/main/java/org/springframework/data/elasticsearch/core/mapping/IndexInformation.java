/*
 * Copyright 2021 the original author or authors.
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

import java.util.Set;

import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.AliasData;
import org.springframework.lang.Nullable;

/**
 * Immutable object that holds information(name, settings, mappings, aliases) about an Index
 *
 * @author George Popides
 * @since 4.2
 */
public class IndexInformation {
	private final String name;
	@Nullable
	private final Document settings;
	@Nullable
	private final Document mappings;
	@Nullable
	private final Set<AliasData> aliases;


	public static IndexInformation create(
			String indexName,
			@Nullable Document settings,
			@Nullable Document mappings,
			@Nullable Set<AliasData> aliases
	) {
		return new IndexInformation(indexName, settings, mappings, aliases);
	}

	private IndexInformation(
			String indexName,
			@Nullable Document settings,
			@Nullable Document mappings,
			@Nullable Set<AliasData> aliases
	) {
		this.name = indexName;
		this.settings = settings;
		this.mappings = mappings;
		this.aliases = aliases;
	}

	public Document getMappings() {
		return mappings;
	}

	public String getName() {
		return name;
	}

	public Document getSettings() {
		return settings;
	}

	public Set<AliasData> getAliases() {
		return aliases;
	}

}
