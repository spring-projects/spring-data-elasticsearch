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

package org.springframework.data.elasticsearch.core;

import java.util.List;

import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.AliasData;
import org.springframework.data.elasticsearch.core.index.Settings;
import org.springframework.lang.Nullable;

/**
 * Immutable object that holds information(name, settings, mappings, aliases) about an Index
 *
 * @author George Popides
 * @author Peter-Josef Meisch
 * @since 4.2
 */
public class IndexInformation {
	private final String name;
	@Nullable private final Settings settings;
	@Nullable private final Document mapping;
	@Nullable private final List<AliasData> aliases;

	public static IndexInformation of(String name, @Nullable Settings settings, @Nullable Document mapping,
			@Nullable List<AliasData> aliases) {
		return new IndexInformation(name, settings, mapping, aliases);
	}

	private IndexInformation(String name, @Nullable Settings settings, @Nullable Document mapping,
			@Nullable List<AliasData> aliases) {
		this.name = name;
		this.settings = settings;
		this.mapping = mapping;
		this.aliases = aliases;
	}

	public String getName() {
		return name;
	}

	@Nullable
	public Document getMapping() {
		return mapping;
	}

	@Nullable
	public Settings getSettings() {
		return settings;
	}

	@Nullable
	public List<AliasData> getAliases() {
		return aliases;
	}
}
