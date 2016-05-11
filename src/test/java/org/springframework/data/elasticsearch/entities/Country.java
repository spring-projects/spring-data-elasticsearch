package org.springframework.data.elasticsearch.entities;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

/**
 * @author Petar Tahchiev
 */
@Document(indexName = "test-country-index", type = "test-country-type", shards = 1, replicas = 0, refreshInterval = "-1")
public class Country {

	private String id;

	private String name;

	private Date founded;

	private GeoPoint coorinates;

	public String getId() {
		return id;
	}

	@Id
	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	@Field(type = FieldType.String, store = true)
	public void setName(String name) {
		this.name = name;
	}

	public Date getFounded() {
		return founded;
	}

	@Field(format = DateFormat.basic_date)
	public void setFounded(Date founded) {
		this.founded = founded;
	}

	public GeoPoint getCoorinates() {
		return coorinates;
	}

	@GeoPointField
	public void setCoorinates(GeoPoint coorinates) {
		this.coorinates = coorinates;
	}
}
