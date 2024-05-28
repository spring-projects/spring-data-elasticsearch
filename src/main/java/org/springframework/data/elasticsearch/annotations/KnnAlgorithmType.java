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
public enum KnnAlgorithmType {
	HNSW("hnsw"),
	INT8_HNSW("int8_hnsw"),
	FLAT("flat"),
	INT8_FLAT("int8_flat"),
	DEFAULT("");

	private final String type;

	KnnAlgorithmType(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}
}
