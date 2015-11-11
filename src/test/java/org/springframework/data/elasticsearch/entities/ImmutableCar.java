/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Artur Konczak
 */
@Document(indexName="cars", type="car")
public final class ImmutableCar {

	@Id
	private final String id;
	private final String name;
	private final String model;

	@JsonCreator
	public ImmutableCar(
			@JsonProperty("id") final String id, 
			@JsonProperty("name") final String name, 
			@JsonProperty("model") final String model) {
		super();
		this.id = id;
		this.name = name;
		this.model = model;
	}

	public String getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}

	public String getModel() {
		return model;
	}
}
