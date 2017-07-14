package org.springframework.data.elasticsearch.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * @author zzt
 */
public class File {

	@Id
	private String id;
	@Field(type = FieldType.text)
	private String title;
	@Field(type = FieldType.text)
	private String content;

}
