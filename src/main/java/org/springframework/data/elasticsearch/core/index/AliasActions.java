/*
 * Copyright 2020-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.core.index;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.lang.Nullable;

/**
 * Class to define to actions to execute in alias management functions.
 * {@see https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-aliases.html}
 *
 * @author Peter-Josef Meisch
 * @since 4.1
 */
public class AliasActions {

	private final List<AliasAction> actions = new ArrayList<>();

	/**
	 * Creates an {@link AliasActions} object with the passed in action elements.
	 *
	 * @param actions {@link AliasAction} elements
	 */
	public AliasActions(@Nullable AliasAction... actions) {
		add(actions);
	}

	public List<AliasAction> getActions() {
		return Collections.unmodifiableList(actions);
	}

	/**
	 * Adds {@link AliasAction} elements to this {@link AliasActions}
	 *
	 * @param actions elements to add
	 * @return this object
	 */
	public AliasActions add(@Nullable AliasAction... actions) {

		if (actions != null) {
			this.actions.addAll(Arrays.asList(actions));
		}

		return this;
	}
}
