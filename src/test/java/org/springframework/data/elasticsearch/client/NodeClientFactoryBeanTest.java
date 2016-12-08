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
package org.springframework.data.elasticsearch.client;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;

import org.elasticsearch.node.Node;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;


/**
 * @author Christophe Friederich
 */
public class NodeClientFactoryBeanTest {

	@Rule public TemporaryFolder folder = new TemporaryFolder(new File("target"));

	/**
	 * Verify Elasticsearch shutdown on Spring destroy life-cycle.
	 * 
	 * @throws Exception occurs when test failed.
	 * @see DATAES-314
	 */
	@Test
	public void shouldESClientShutdown() throws Exception {
		Path homeFolder = folder.newFolder().toPath();
		NodeClientFactoryBean clientFactoryBean = new NodeClientFactoryBean(true);
		clientFactoryBean.setClusterName(UUID.randomUUID().toString());
		clientFactoryBean.setEnableHttp(false);
		clientFactoryBean.setPathData(homeFolder.resolve("index").toAbsolutePath().toString());
		clientFactoryBean.setPathHome(homeFolder.toAbsolutePath().toString());
		// initialize client and start node
		clientFactoryBean.afterPropertiesSet();
		// verify node has been created and started
		Node node = clientFactoryBean.getNode();
		assertThat(node, notNullValue());
		assertThat(node.isClosed(), is(false));
		// close node and client
		clientFactoryBean.destroy();
		// verify node is closed.
		assertThat(node.isClosed(), is(true));
	}

}
