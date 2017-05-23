package org.springframework.data.elasticsearch;

import org.elasticsearch.action.admin.indices.close.CloseIndexRequest;
import org.elasticsearch.action.admin.indices.close.CloseIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeValidationException;
import org.elasticsearch.node.internal.InternalSettingsPreparer;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.transport.Netty4Plugin;
import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static java.util.Arrays.asList;

public class ESServer {

    private static class MyNode extends Node {
        public MyNode(Settings preparedSettings, Collection<Class<? extends Plugin>> classpathPlugins) {
            super(InternalSettingsPreparer.prepareEnvironment(preparedSettings, null), classpathPlugins);
        }
    }

    @Test
    @Ignore
    public void shouldDo() throws NodeValidationException, IOException, ExecutionException, InterruptedException {

        Node node = new MyNode(
                Settings.builder()
                        .put("transport.type", "netty4")
                        .put("transport.tcp.port", "9300")
                        .put("http.type", "netty4")
                        .put("http.enabled", "true")
                        .put("path.home", "elasticsearch-data")
                        .build(),
                Arrays.<Class<? extends Plugin>>asList(Netty4Plugin.class));
        node.start();

        String localNodeId = node.client().admin().cluster().prepareState().get().getState().getNodes().getLocalNodeId();
        String value = node.client().admin().cluster().prepareNodesInfo(localNodeId).get().getNodes().iterator().next().getHttp().address().publishAddress().toString();
        System.out.println(localNodeId);
        System.out.println(value);
        System.out.println(node.client().admin().cluster().prepareNodesInfo(localNodeId).get().getNodes().iterator().next().getTransport().address().publishAddress().toString());

        final Client client = node.client();
        //final boolean test = client.admin().indices().create(new CreateIndexRequest("test1")).get().isAcknowledged();
        //System.out.println(true);

        URL url = new URL("http://localhost:9200");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();


        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        node.close();

        System.out.println(response.toString());

        System.out.println("aa");

        //given
//        Properties props = new Properties();
//        props.setProperty("path.home", "data");
//        props.setProperty("path.data", "data");
//        props.setProperty("http.port", "9200");
//        props.setProperty("transport.tcp.port", "9400");
//        props.setProperty("cluster.name", "test-node");
//        props.setProperty("transport.type", "local");
//        props.setProperty("discovery.type", "local");
//        props.setProperty("http.type", "netty4");
//        Settings settings = Settings.builder().put(props).build();
//        Node node = new Node(InternalSettingsPreparer.prepareEnvironment(settings,null));
//
//        try {
//            node.start();
//        } catch (NodeValidationException e) {
//            e.printStackTrace();
//        }
//
//        try {
//            node.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        //when

        //then

    }


}
