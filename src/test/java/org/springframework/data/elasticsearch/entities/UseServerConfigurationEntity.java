package org.springframework.data.elasticsearch.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

/**
 * Created by akonczak on 12/12/2015.
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = "test-index-server-configuration", type = "test-type", useServerConfiguration = true, shards = 10, replicas = 10, refreshInterval = "-1")
public class UseServerConfigurationEntity {

	@Id
	private String id;

	private String val;

}
