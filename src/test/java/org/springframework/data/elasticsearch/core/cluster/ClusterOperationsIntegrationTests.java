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
package org.springframework.data.elasticsearch.core.cluster;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;

/**
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
public abstract class ClusterOperationsIntegrationTests {

	@Autowired private ElasticsearchOperations operations;
	private ClusterOperations clusterOperations;

	@BeforeEach
	void setUp() {
		clusterOperations = operations.cluster();
	}

	@Test // #1390
	@DisplayName("should return cluster health information")
	void shouldReturnClusterHealthInformation() {

		ClusterHealth clusterHealth = clusterOperations.health();

		List<String> allowedStates = Arrays.asList("GREEN", "YELLOW");
		assertThat(allowedStates).contains(clusterHealth.getStatus());
	}
}
