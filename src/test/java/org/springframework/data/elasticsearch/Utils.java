/*
 * Copyright 2015-2019 the original author or authors.
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
package org.springframework.data.elasticsearch;

import static java.util.Arrays.*;

import java.util.UUID;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.NodeValidationException;
import org.elasticsearch.transport.Netty4Plugin;

import org.springframework.data.elasticsearch.client.NodeClientFactoryBean;

/**
 * @author Mohsin Husen
 * @author Artur Konczak
 * @author Ilkang Na
 */
public class Utils {

	public static Client getNodeClient() throws NodeValidationException {

		String pathHome = "src/test/resources/test-home-dir";
		String pathData = "target/elasticsearchTestData";
		String clusterName = UUID.randomUUID().toString();

		return new NodeClientFactoryBean.TestNode(Settings.builder().put("transport.type", "netty4")
				.put("http.type", "netty4").put("path.home", pathHome).put("path.data", pathData)
				.put("cluster.name", clusterName).put("node.max_local_storage_nodes", 100).build(), asList(Netty4Plugin.class))
						.start().client();
	}
}
