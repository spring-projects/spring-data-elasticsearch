/*
 * Copyright 2016-2022 the original author or authors.
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
package org.springframework.data.elasticsearch.repositories.cdi;

import org.springframework.data.repository.CrudRepository;

/**
 * @author Mark Paluch
 * @see <a href="https://jira.spring.io/browse/DATAES-234">DATAES-234</a>
 */
@PersonDB
@OtherQualifier
public interface QualifiedProductRepository extends CrudRepository<CdiRepositoryTests.Product, String> {

}
