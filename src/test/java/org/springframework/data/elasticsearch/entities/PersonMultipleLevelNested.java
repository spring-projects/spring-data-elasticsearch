/*
 * Copyright 2014 the original author or authors.
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


import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Artur Konczak
 */

@Document(indexName = "test-index-person-multiple-level-nested", type = "user", shards = 1, replicas = 0, refreshInterval = "-1")
public class PersonMultipleLevelNested {

	@Id
	private String id;

	private String name;

	@Field(type = FieldType.Nested)
	private List<GirlFriend> girlFriends;

	@Field(type = FieldType.Nested)
	private List<Car> cars;

	@Field(type = FieldType.Nested, includeInParent = true)
	private List<Car> bestCars;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<GirlFriend> getGirlFriends() {
		return girlFriends;
	}

	public void setGirlFriends(List<GirlFriend> girlFriends) {
		this.girlFriends = girlFriends;
	}

	public List<Car> getCars() {
		return cars;
	}

	public void setCars(List<Car> cars) {
		this.cars = cars;
	}

	public List<Car> getBestCars() {
		return bestCars;
	}

	public void setBestCars(List<Car> bestCars) {
		this.bestCars = bestCars;
	}
}
