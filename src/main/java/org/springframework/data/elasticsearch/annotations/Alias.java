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

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * Identifies an alias for the index.
 *
 * @author Youssef Aouichaoui
 * @since 5.4
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
@Repeatable(Aliases.class)
public @interface Alias {
	/**
	 * @return Index alias name. Alias for {@link #alias}.
	 */
	@AliasFor("alias")
	String value() default "";

	/**
	 * @return Index alias name. Alias for {@link #value}.
	 */
	@AliasFor("value")
	String alias() default "";

	/**
	 * @return Query used to limit documents the alias can access.
	 */
	Filter filter() default @Filter;

	/**
	 * @return Used to route indexing operations to a specific shard.
	 */
	String indexRouting() default "";

	/**
	 * @return Used to route indexing and search operations to a specific shard.
	 */
	String routing() default "";

	/**
	 * @return Used to route search operations to a specific shard.
	 */
	String searchRouting() default "";

	/**
	 * @return Is the alias hidden?
	 */
	boolean isHidden() default false;

	/**
	 * @return Is it the 'write index' for the alias?
	 */
	boolean isWriteIndex() default false;
}
