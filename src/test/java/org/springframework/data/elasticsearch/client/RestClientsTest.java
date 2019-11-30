package org.springframework.data.elasticsearch.client;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import java.io.IOException;

import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

/**
 * @author Peter-Josef Meisch
 */
public class RestClientsTest {

	@Test // DATAES-700
	void shouldUseConfiguredProxy() throws IOException {

		WireMockServer wireMockServer = new WireMockServer(options() //
				.dynamicPort() //
				.usingFilesUnderDirectory("src/test/resources/wiremock-mappings")); // needed, otherwise Wiremock goes to
																					// test/resources/mappings
		wireMockServer.start();
		try {
			WireMock.configureFor(wireMockServer.port());

			ClientConfigurationBuilder configurationBuilder = new ClientConfigurationBuilder();
			ClientConfiguration clientConfiguration = configurationBuilder //
					.connectedTo("localhost:9200")//
					.withProxy("localhost:" + wireMockServer.port()) //
					.build();

			RestHighLevelClient restClient = RestClients.create(clientConfiguration).rest();
			restClient.ping(RequestOptions.DEFAULT);

			verify(headRequestedFor(urlEqualTo("/")));

		} finally {
			wireMockServer.shutdown();
		}
	}

}
