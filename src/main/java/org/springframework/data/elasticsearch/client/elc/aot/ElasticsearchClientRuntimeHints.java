/*
 * Copyright 2023-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.client.elc.aot;

import co.elastic.clients.elasticsearch._types.mapping.RuntimeFieldType;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.elasticsearch.indices.PutMappingRequest;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.lang.Nullable;

/**
 * runtime hints for the Elasticsearch client libraries, as these do not provide any of their own.
 *
 * @author Peter-Josef Meisch
 * @since 5.1
 */
public class ElasticsearchClientRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {

		hints.reflection()
				.registerType(TypeReference.of(IndexSettings.class), builder -> builder.withField("_DESERIALIZER")) //
				.registerType(TypeReference.of(PutMappingRequest.class), builder -> builder.withField("_DESERIALIZER")) //
				.registerType(TypeReference.of(RuntimeFieldType.class), builder -> builder.withField("_DESERIALIZER"))//
				.registerType(TypeReference.of(TypeMapping.class), builder -> builder.withField("_DESERIALIZER")) //
		;

		hints.serialization() //
				.registerType(org.apache.http.impl.auth.BasicScheme.class) //
				.registerType(org.apache.http.impl.auth.RFC2617Scheme.class) //
				.registerType(java.util.HashMap.class) //
		;

		hints.resources() //
				.registerPattern("co/elastic/clients/version.properties") //
		;
	}
}
