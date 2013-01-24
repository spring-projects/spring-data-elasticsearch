package org.springframework.data.elasticsearch.client;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.node.NodeClient;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

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
