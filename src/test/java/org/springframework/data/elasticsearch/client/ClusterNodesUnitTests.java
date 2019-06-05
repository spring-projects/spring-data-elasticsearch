/*
 * Copyright 2018-2019 the original author or authors.
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
package org.springframework.data.elasticsearch.client;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

/**
 * Unit tests for {@link ClusterNodes}.
 * 
 * @author Oliver Gierke
 */
public class ClusterNodesUnitTests {

	@Test // DATAES-470
	public void parsesSingleClusterNode() {

		ClusterNodes nodes = ClusterNodes.DEFAULT;

		assertThat(nodes).hasSize(1) //
				.first().satisfies(it -> {
					assertThat(it.getAddress()).isEqualTo("127.0.0.1");
					assertThat(it.getPort()).isEqualTo(9300);
				});
	}

	@Test // DATAES-470
	public void parsesMultiClusterNode() {

		ClusterNodes nodes = ClusterNodes.of("127.0.0.1:1234,10.1.0.1:5678");

		assertThat(nodes.stream()).hasSize(2); //
		assertThat(nodes.stream()).element(0).satisfies(it -> {
			assertThat(it.getAddress()).isEqualTo("127.0.0.1");
			assertThat(it.getPort()).isEqualTo(1234);
		});
		assertThat(nodes.stream()).element(1).satisfies(it -> {
			assertThat(it.getAddress()).isEqualTo("10.1.0.1");
			assertThat(it.getPort()).isEqualTo(5678);
		});
	}

	@Test // DATAES-470
	public void rejectsEmptyHostName() {

		assertThatExceptionOfType(IllegalArgumentException.class) //
				.isThrownBy(() -> ClusterNodes.of(":8080")) //
				.withMessageContaining("host");
	}

	@Test // DATAES-470
	public void rejectsEmptyPort() {

		assertThatExceptionOfType(IllegalArgumentException.class) //
				.isThrownBy(() -> ClusterNodes.of("localhost:")) //
				.withMessageContaining("port");
	}

	@Test // DATAES-470
	public void rejectsMissingPort() {

		assertThatExceptionOfType(IllegalArgumentException.class) //
				.isThrownBy(() -> ClusterNodes.of("localhost")) //
				.withMessageContaining("host:port");
	}

	@Test // DATAES-470
	public void rejectsUnresolvableHost() {

		assertThatExceptionOfType(IllegalArgumentException.class) //
				.isThrownBy(() -> ClusterNodes.of("mylocalhost.invalid.:80"));
	}
}
