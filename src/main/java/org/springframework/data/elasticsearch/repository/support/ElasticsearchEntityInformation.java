/*
 * Copyright 2013-2021 the original author or authors.
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
package org.springframework.data.elasticsearch.repository.support;

import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.lang.Nullable;

/**
 * @param <T>
 * @param <ID>
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Christoph Strobl
 * @author Ivan Greene
 * @author Peter-Josef Meisch
 * @author Aleksei Arsenev
 */
public interface ElasticsearchEntityInformation<T, ID> extends EntityInformation<T, ID> {

	String getIdAttribute();

	IndexCoordinates getIndexCoordinates();

	@Nullable
	Long getVersion(T entity);

	@Nullable
	Document.VersionType getVersionType();
}
