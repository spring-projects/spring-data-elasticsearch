package org.springframework.data.elasticsearch;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Document(indexName = "index", type = "type")
public class MinimalEntity {
	@Id
	private String id;
}
