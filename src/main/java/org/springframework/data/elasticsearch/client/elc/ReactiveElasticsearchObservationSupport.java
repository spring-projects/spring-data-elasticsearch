/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.data.elasticsearch.client.elc;

import io.micrometer.observation.Observation;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import org.jspecify.annotations.Nullable;

import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

/**
 * Observation lifecycle shared by ELC templates and their child templates.
 *
 * @author Ralph Ursprung
 * @author Peter-Josef Meisch
 * @since 6.2
 */
@FunctionalInterface
interface ReactiveElasticsearchObservationSupport extends ElasticsearchObservationSupport {

	default <T> Mono<T> observeMono(ElasticsearchOperationName operation, @Nullable IndexCoordinates index,
			@Nullable Integer batchSize, Mono<T> mono) {
		return Mono.deferContextual(contextView -> {
			var observation = createObservation(operation, index, batchSize);
			setParent(observation, contextView);
			observation.start();
			return mono.doOnError(observation::error)
					.doFinally(signalType -> observation.stop())
					.contextWrite(context -> context.put(ObservationThreadLocalAccessor.KEY, observation));
		});
	}

	default <T> Flux<T> observeFlux(ElasticsearchOperationName operation, @Nullable IndexCoordinates index,
			Flux<T> flux) {
		return Flux.deferContextual(contextView -> {
			var observation = createObservation(operation, index, null);
			setParent(observation, contextView);
			observation.start();
			return flux.doOnError(observation::error)
					.doFinally(signalType -> observation.stop())
					.contextWrite(context -> context.put(ObservationThreadLocalAccessor.KEY, observation));
		});
	}

	private static void setParent(Observation observation, ContextView contextView) {
		var parent = contextView.<Observation> getOrDefault(ObservationThreadLocalAccessor.KEY, null);
		if (parent != null) {
			observation.parentObservation(parent);
		}
	}
}
