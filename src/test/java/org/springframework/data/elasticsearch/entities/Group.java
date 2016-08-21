package org.springframework.data.elasticsearch.entities;

import java.util.HashSet;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * Created by akonczak on 21/08/2016.
 */

@Document(indexName = "groups", type = "group")
public class Group {

	@Id
	String id;

	@Field(type = FieldType.Nested, ignoreFields ={"groups"})
	private Set<User> users = new HashSet<User>();
}
