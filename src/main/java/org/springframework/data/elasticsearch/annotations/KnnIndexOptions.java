/*
 * Copyright 2024 the original author or authors.
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

/**
 * @author Haibo Liu
 * @since 5.4
 */
public @interface KnnIndexOptions {

	KnnAlgorithmType type() default KnnAlgorithmType.DEFAULT;

	/**
	 * Only applicable to {@link KnnAlgorithmType#HNSW} and {@link KnnAlgorithmType#INT8_HNSW} index types.
	 */
	int m() default -1;

	/**
	 * Only applicable to {@link KnnAlgorithmType#HNSW} and {@link KnnAlgorithmType#INT8_HNSW} index types.
	 */
	int efConstruction() default -1;

	/**
	 * Only applicable to {@link KnnAlgorithmType#INT8_HNSW} and {@link KnnAlgorithmType#INT8_FLAT} index types.
	 */
	float confidenceInterval() default -1F;
}
