package org.springframework.data.elasticsearch.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.GeoPointField;
import org.springframework.data.elasticsearch.annotations.GeoShapeField;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.geo.GeoShape;
import org.springframework.data.elasticsearch.core.geo.GeoShapeGeometryCollection;
import org.springframework.data.elasticsearch.core.geo.GeoShapeLinestring;
import org.springframework.data.elasticsearch.core.geo.GeoShapeMultiLinestring;
import org.springframework.data.elasticsearch.core.geo.GeoShapeMultiPoint;
import org.springframework.data.elasticsearch.core.geo.GeoShapeMultiPolygon;
import org.springframework.data.elasticsearch.core.geo.GeoShapePoint;
import org.springframework.data.elasticsearch.core.geo.GeoShapePolygon;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Point;
import org.springframework.data.geo.Polygon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Artur Konczak
 * @author Lukas Vorisek
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

	//geo shape - Custom implementation + Spring data
	@GeoShapeField(precision = "5m")
	private Polygon polygonA;

	private GeoShapePolygon polygonB;

	private GeoShape polygonC;

	private GeoShapeLinestring linestringA;

	private GeoShape linestringB;

	private GeoShapePoint geoshapePointA;

	private GeoShape geoshapePointB;

	private GeoShapeMultiPoint multipointA;

	private GeoShape multipointB;

	private GeoShapeMultiPolygon multipolygonA;

	private GeoShape multipolygonB;

	private GeoShapeMultiLinestring multilinestringA;

	private GeoShape multilinestringB;

	private GeoShapeGeometryCollection collectionA;

	private GeoShape collectionB;

	//geo point - Custom implementation + Spring Data
	@GeoPointField
	private Point pointA;

	private GeoPoint pointB;

	@GeoPointField
	private String pointC;

	@GeoPointField
	private double[] pointD;
}
