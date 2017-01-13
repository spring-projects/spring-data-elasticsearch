/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.data.elasticsearch;

import java.util.UUID;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeValidationException;

/**
 * @author Mohsin Husen
 */
public class Utils {

	public static NodeClient getNodeClient() throws NodeValidationException {
		String name = UUID.randomUUID().toString();
		Settings settings = Settings.builder()
			.put("http.enabled", "false")
			.put("path.data", "target/elasticsearchTestData")
			.put("path.home", "src/test/resources/test-home-dir")
			.put("cluster.name", name)
			.put("node.name", name)
			.put("node.local_storage", true)
			.put("transport.type", "local")
			.put("node.max_local_storage_nodes", "20")
			.build();
		return (NodeClient) new Node(settings).start().client();
	}
}
