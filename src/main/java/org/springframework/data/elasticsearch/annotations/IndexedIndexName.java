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
package org.springframework.data.elasticsearch.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a String property of an entity to be filled with the name of the index where the entity was stored
 * after it is indexed into Elasticsearch. This can be used when the name of the index is dynamically created or when a
 * document was indexed into a write alias.
 * <p>
 * This can not be used to specify the index where an entity should be written to.
 *
 * @author Peter-Josef Meisch
 * @since 5.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Documented
@Field(type = FieldType.Auto) // prevents the property being written to the index mapping
public @interface IndexedIndexName {
}
