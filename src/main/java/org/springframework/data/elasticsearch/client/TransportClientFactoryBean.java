package org.springframework.data.elasticsearch.client;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.util.Properties;

import static org.apache.commons.lang.StringUtils.substringAfter;
import static org.apache.commons.lang.StringUtils.substringBefore;
import static org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder;


public class TransportClientFactoryBean implements FactoryBean<TransportClient>, InitializingBean, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(TransportClientFactoryBean.class);
    private String[] clusterNodes;
    private TransportClient client;
    private Properties properties;
    static final String COLON = ":";

    @Override
    public void destroy() throws Exception {
        try {
            logger.info("Closing elasticSearch  client");
            if (client != null) {
                client.close();
            }
        } catch (final Exception e) {
            logger.error("Error closing ElasticSearch client: ", e);
        }
    }

    @Override
    public TransportClient getObject() throws Exception {
        return client;
    }

    @Override
    public Class<TransportClient> getObjectType() {
        return TransportClient.class;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        buildClient();
    }

    protected void buildClient() throws Exception {
        client =  new TransportClient(settings());
        Assert.notEmpty(clusterNodes,"[Assertion failed] clusterNodes settings missing.");
        for (String clusterNode : clusterNodes) {
            String hostName = substringBefore(clusterNode, COLON);
            String port = substringAfter(clusterNode, COLON);
            Assert.hasText(hostName,"[Assertion failed] missing host name in 'clusterNodes'");
            Assert.hasText(port,"[Assertion failed] missing port in 'clusterNodes'");
            logger.info("adding transport node : " + clusterNode);
            client.addTransportAddress(new InetSocketTransportAddress(hostName, Integer.valueOf(port)));
        }
        client.connectedNodes();
    }

    private Settings settings(){
        if(properties != null){
            return settingsBuilder().put(properties).build();
        }
        return settingsBuilder()
                .put("client.transport.sniff",true).build();
    }

    public void setClusterNodes(String[] clusterNodes) {
        this.clusterNodes = clusterNodes;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}
