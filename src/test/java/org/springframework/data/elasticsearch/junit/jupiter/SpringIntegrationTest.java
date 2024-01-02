/*
 * Copyright 2019-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.junit.jupiter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Combines the {@link SpringDataElasticsearchExtension} and the {@link SpringExtension}. Tests are executed in
 * accordance to the {@link org.junit.jupiter.api.Order} annotation, to be able to have an explicit last test method for
 * cleanup (since 4.3)
 *
 * @author Peter-Josef Meisch
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ExtendWith(SpringDataElasticsearchExtension.class)
@ExtendWith(SpringExtension.class)
@Tag(Tags.INTEGRATION_TEST)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public @interface SpringIntegrationTest {
}
