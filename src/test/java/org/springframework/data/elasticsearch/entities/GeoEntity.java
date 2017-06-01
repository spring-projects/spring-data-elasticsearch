package org.springframework.data.elasticsearch.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.GeoPointField;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Point;
import org.springframework.data.geo.Polygon;

/**
 * @author Artur Konczak
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = "test-index-geo", type = "geo-test-index", shards = 1, replicas = 0, refreshInterval = "-1")
public class GeoEntity {

	@Id
	private String id;

	//geo shape - Spring Data
	private Box box;
	private Circle circle;
	private Polygon polygon;

	//geo point - Custom implementation + Spring Data
	@GeoPointField
	private Point pointA;

	private GeoPoint pointB;

	@GeoPointField
	private String pointC;

	@GeoPointField
	private double[] pointD;
}
