/*
 * Copyright 2026-present the original author or authors.
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

import org.springframework.data.elasticsearch.core.index.IndexOptionMapper;

/**
 * Represents the custom index option, either not provided by the core (fe, custom plugin)
 * or deviates across different engines.
 * 
 * @author Andriy Redko
 * 
 * @since 6.2
 */
public @interface CustomIndexOption {
    /**
     * The name of the custom index option 
     */
    String name();

    /**
     * The value(s) of the custom index option 
     */
    String[] values() default {};

    /**
     * Should the index property be overridden if already present or not 
     */
    boolean overrideIfPresent() default false;
    
    /**
     * The index option mapper that will be used to populate this custom index option
     */
    Class<? extends IndexOptionMapper> mapper();
}
