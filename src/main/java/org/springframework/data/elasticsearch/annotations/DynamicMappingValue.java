/*
 * Copyright 2019-2021 the original author or authors.
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
 * values for the {@link DynamicMapping annotation}
 *
 * @author Peter-Josef Meisch
 * @author Sascha Woo
 * @since 4.0
 * @deprecated since 4.3, use {@link Document#dynamic()} or {@link Field#dynamic()} instead.
 */
@Deprecated
public enum DynamicMappingValue {
	True("true"), False("false"), Strict("strict");

	private final String mappedName;

	DynamicMappingValue(String mappedName) {
		this.mappedName = mappedName;
	}

	public String getMappedName() {
		return mappedName;
	}
}
