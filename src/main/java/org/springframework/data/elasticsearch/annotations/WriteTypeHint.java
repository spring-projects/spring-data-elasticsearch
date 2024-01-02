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
package org.springframework.data.elasticsearch.annotations;

import org.springframework.data.mapping.context.MappingContext;

/**
 * Defines if type hints should be written. Used by {@link Document} annotation.
 *
 * @author Peter-Josef Meisch
 * @since 4.3
 */
public enum WriteTypeHint {

	/**
	 * Use the global settings from the {@link MappingContext}.
	 */
	DEFAULT,
	/**
	 * Always write type hints for the entity.
	 */
	TRUE,
	/**
	 * Never write type hints for the entity.
	 */
	FALSE
}
