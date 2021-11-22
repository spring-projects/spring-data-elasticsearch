/*
 * Copyright 2014-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.repositories.cdi;

import jakarta.inject.Inject;

import org.springframework.lang.Nullable;

/**
 * @author Mohsin Husen
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Peter-Josef Meisch
 */
class CdiRepositoryClient {

	@Nullable private CdiProductRepository repository;
	@Nullable private SamplePersonRepository samplePersonRepository;
	@Nullable private QualifiedProductRepository qualifiedProductRepository;

	@Nullable
	public CdiProductRepository getRepository() {
		return repository;
	}

	@Inject
	public void setRepository(CdiProductRepository repository) {
		this.repository = repository;
	}

	@Nullable
	public SamplePersonRepository getSamplePersonRepository() {
		return samplePersonRepository;
	}

	@Inject
	public void setSamplePersonRepository(SamplePersonRepository samplePersonRepository) {
		this.samplePersonRepository = samplePersonRepository;
	}

	@Nullable
	public QualifiedProductRepository getQualifiedProductRepository() {
		return qualifiedProductRepository;
	}

	@Inject
	public void setQualifiedProductRepository(
			@PersonDB @OtherQualifier QualifiedProductRepository qualifiedProductRepository) {
		this.qualifiedProductRepository = qualifiedProductRepository;
	}
}
