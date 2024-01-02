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
package org.springframework.data.elasticsearch.repository.aot;

import static org.springframework.data.elasticsearch.aot.ElasticsearchAotPredicates.*;

import java.util.Arrays;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.lang.Nullable;

/**
 * @author Peter-Josef Meisch
 * @since 5.1
 */
public class RepositoryRuntimeHints implements RuntimeHintsRegistrar {
	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		hints.reflection().registerTypes(
				Arrays.asList(TypeReference
						.of("org.springframework.data.elasticsearch.repository.support.SimpleElasticsearchRepository")),
				builder -> builder.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
						MemberCategory.INVOKE_PUBLIC_METHODS));

		if (isReactorPresent()) {
			hints.reflection().registerTypes(
					Arrays.asList(TypeReference
							.of("org.springframework.data.elasticsearch.repository.support.SimpleReactiveElasticsearchRepository")),
					builder -> builder.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
							MemberCategory.INVOKE_PUBLIC_METHODS));

		}
	}
}
