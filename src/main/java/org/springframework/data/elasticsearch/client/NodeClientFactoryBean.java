package org.springframework.data.elasticsearch.client;

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author: withccm
 * https://github.com/spring-projects/spring-data-elasticsearch/pull/170
 */
public class NodeClientFactoryBean implements FactoryBean<NodeClient>, InitializingBean, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(NodeClientFactoryBean.class);
    private boolean local;
    private boolean enableHttp;
    private String clusterName;
    private NodeClient nodeClient;
    private Node node;
    private String pathData;
    private String pathHome;
    private String pathConfiguration;

    NodeClientFactoryBean() {
    }

    public NodeClientFactoryBean(boolean local) {
        this.local = local;
    }


    /**
     * Visible for test only
     */
    Node getNode() {
        return node;
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

        Settings settings = Settings.builder().put(loadConfig())
                			.put("http.enabled", String.valueOf(this.enableHttp))
                			.put("path.home", this.pathHome)
                			.put("path.data", this.pathData)
                			.put("cluster.name", this.clusterName)
                			.put("node.name", this.clusterName)
                			.put("node.local_storage", this.local)
                			.put("transport.type", "local")
                			.put("node.max_local_storage_nodes", "20")
                			.build();
        node = new Node(settings).start();
        nodeClient = (NodeClient)node.client();
    }

    private Settings loadConfig() throws IOException {
        if (StringUtils.isNotBlank(pathConfiguration)) {
            InputStream stream = getClass().getClassLoader().getResourceAsStream(pathConfiguration);
            if (stream != null) {
                return Settings.builder().loadFromStream(pathConfiguration, getClass().getClassLoader().getResourceAsStream(pathConfiguration)).build();
            }
            logger.error(String.format("Unable to read node configuration from file [%s]", pathConfiguration));
        }
        return Settings.builder().build();
    }

    public void setLocal(boolean local) {
        this.local = local;
    }

    public void setEnableHttp(boolean enableHttp) {
        this.enableHttp = enableHttp;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public void setPathData(String pathData) {
        this.pathData = pathData;
    }

    public void setPathHome(String pathHome) {
        this.pathHome = pathHome;
    }

    public void setPathConfiguration(String configuration) {
        this.pathConfiguration = configuration;
    }

    @Override
    public void destroy() throws Exception {
        try {
            logger.info("Closing elasticSearch  client");
            if (nodeClient != null) {
                nodeClient.close();
            }
            if (node != null) {
                node.close();
            }
        } catch (final Exception e) {
            logger.error("Error closing ElasticSearch client: ", e);
        }
    }
}
