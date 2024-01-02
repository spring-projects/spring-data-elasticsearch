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
package org.springframework.data.elasticsearch.core.convert;

import org.springframework.data.elasticsearch.core.mapping.PropertyValueConverter;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.util.Assert;

/**
 * @author Sascha Woo
 * @since 4.3
 */
public abstract class AbstractPropertyValueConverter implements PropertyValueConverter {

	private final PersistentProperty<?> property;

	public AbstractPropertyValueConverter(PersistentProperty<?> property) {

		Assert.notNull(property, "property must not be null.");
		this.property = property;
	}

	protected PersistentProperty<?> getProperty() {
		return property;
	}

}
