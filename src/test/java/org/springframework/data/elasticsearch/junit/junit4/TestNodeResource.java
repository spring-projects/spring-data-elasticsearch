/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.data.elasticsearch.junit.junit4;

import java.io.IOException;

import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.junit.rules.ExternalResource;
import org.springframework.data.elasticsearch.Utils;
import org.springframework.util.Assert;

/**
 * JUnit4 Rule that sets up and tears down a local Elasticsearch node.
 *
 * @author Peter-Josef Meisch
 */
public class TestNodeResource extends ExternalResource {

	private Node node;

	@Override
	protected void before() throws Throwable {
		node = Utils.getNode();
		node.start();
	}

	@Override
	protected void after() {
		if (node != null) {
			try {
				node.close();
			} catch (IOException ignored) {}
		}
	}

	public Client client() {
		Assert.notNull(node, "node is not initialized");
		return node.client();
	}
}
