/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.client;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.node.NodeClient;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

/**
 *  NodeClientFactoryBean
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 */

public class NodeClientFactoryBean implements FactoryBean<NodeClient>, InitializingBean{

    private boolean local;
    private NodeClient nodeClient;

    NodeClientFactoryBean() {
    }

    public NodeClientFactoryBean(boolean local) {
        this.local = local;
    }

    @Override
    public NodeClient getObject() throws Exception {
        return nodeClient;
    }

    @Override
    public Class<? extends Client> getObjectType() {
        return NodeClient.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        nodeClient = (NodeClient) nodeBuilder().local(this.local).node().client();
    }

    public void setLocal(boolean local) {
        this.local = local;
    }
}
