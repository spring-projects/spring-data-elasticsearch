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
package org.springframework.data.elasticsearch.aot;

import static org.springframework.data.elasticsearch.aot.ElasticsearchAotPredicates.*;

import java.util.Arrays;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.data.elasticsearch.client.elc.EntityAsMap;
import org.springframework.data.elasticsearch.core.event.AfterConvertCallback;
import org.springframework.data.elasticsearch.core.event.AfterLoadCallback;
import org.springframework.data.elasticsearch.core.event.AfterSaveCallback;
import org.springframework.data.elasticsearch.core.event.BeforeConvertCallback;
import org.springframework.data.elasticsearch.core.event.ReactiveAfterConvertCallback;
import org.springframework.data.elasticsearch.core.event.ReactiveAfterLoadCallback;
import org.springframework.data.elasticsearch.core.event.ReactiveAfterSaveCallback;
import org.springframework.data.elasticsearch.core.event.ReactiveBeforeConvertCallback;
import org.springframework.lang.Nullable;

/**
 * @author Peter-Josef Meisch
 * @since 5.1
 */
public class SpringDataElasticsearchRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		hints.reflection().registerTypes( //
				Arrays.asList( //
						TypeReference.of(AfterConvertCallback.class), //
						TypeReference.of(AfterLoadCallback.class), //
						TypeReference.of(AfterSaveCallback.class), //
						TypeReference.of(BeforeConvertCallback.class), //
						TypeReference.of(EntityAsMap.class) //
				), //
				builder -> builder.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
						MemberCategory.INVOKE_PUBLIC_METHODS));

		if (isReactorPresent()) {
			hints.reflection().registerTypes( //
					Arrays.asList( //
							TypeReference.of(ReactiveAfterConvertCallback.class), //
							TypeReference.of(ReactiveAfterLoadCallback.class), //
							TypeReference.of(ReactiveAfterSaveCallback.class), //
							TypeReference.of(ReactiveBeforeConvertCallback.class) //
					), //
					builder -> builder.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
							MemberCategory.INVOKE_PUBLIC_METHODS));
		}

		// properties needed to log the different versions
		hints.resources().registerPattern("versions.properties");
	}
}
