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

import org.springframework.util.Assert;

/**
 * A single action to be contained in {@link AliasActions}.
 *
 * @author Peter-Josef Meisch
 * @since 4.1
 */
public abstract class AliasAction {

	private final AliasActionParameters parameters;

	protected AliasAction(AliasActionParameters parameters) {

		Assert.notNull(parameters, "parameters must not be null");
		Assert.notEmpty(parameters.getIndices(), "parameters must have an indexname set");

		this.parameters = parameters;
	}

	public AliasActionParameters getParameters() {
		return parameters;
	}

	/**
	 * {@link AliasAction} to add an alias.
	 */
	public static class Add extends AliasAction {

		public Add(AliasActionParameters parameters) {
			super(parameters);
		}
	}

	/**
	 * {@link AliasAction} to remove an alias.
	 */
	public static class Remove extends AliasAction {

		public Remove(AliasActionParameters parameters) {
			super(parameters);
		}
	}

	/**
	 * {@link AliasAction} to remove an index.
	 */
	public static class RemoveIndex extends AliasAction {

		public RemoveIndex(AliasActionParameters parameters) {
			super(parameters);
		}
	}

}
