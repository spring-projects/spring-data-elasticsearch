/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core.geo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;

import org.springframework.data.elasticsearch.core.geo.GeoShape.Coordinate;
import org.springframework.data.elasticsearch.core.geo.GeoShape.GeoShapeType;
import org.springframework.data.geo.Point;
import org.springframework.data.geo.Polygon;
import org.springframework.util.Assert;

/**
 * A Jackson {@link Module} to register custom {@link JsonSerializer} and {@link JsonDeserializer}s for GeoShape types.
 *
 * Modification of GeoJsonModule from Spring Data MongoDB.
 * @author Lukas Vorisek
 * @since 3.0
 */
public class GeoShapeModule extends SimpleModule {

	private static final long serialVersionUID = -8723016728655643720L;


	public GeoShapeModule() {

		// Basic
		addSerializer(Polygon.class, new PolygonSerializer());
		addDeserializer(Polygon.class, new PolygonDeserializer());
		addSerializer(Coordinate.class, new CoordinateSerializer());
		addDeserializer(Coordinate.class, new CoordinateDeerializer());
		addSerializer(GeoShapeType.class, new GeoShapeTypeSerializer());
		addDeserializer(GeoShapeType.class, new GeoShapeTypeDeserializer());
		addDeserializer(GeoShape.class, new TypeGeoShapeDeserializer());

		// serializers
		addSerializer(GeoShapePolygon.class, new GeoShapeSerializer<GeoShapePolygon>());
		addSerializer(GeoShapeMultiPolygon.class, new GeoShapeSerializer<GeoShapeMultiPolygon>());
		addSerializer(GeoShapeMultiLinestring.class, new GeoShapeSerializer<GeoShapeMultiLinestring>());
		addSerializer(GeoShapePoint.class, new GeoShapeSerializer<GeoShapePoint>());
		addSerializer(GeoShapeGeometryCollection.class, new GeoShapeGeometryCollectionSerializer());

		// deserializers
		addDeserializer(GeoShapePolygon.class, new GeoShapePolygonDeserializer());
		addDeserializer(GeoShapeMultiPolygon.class, new GeoShapeMultiPolygonDeserializer());
		addDeserializer(GeoShapeMultiLinestring.class, new GeoShapeMultiLinestringDeserializer());
		addDeserializer(GeoShapeGeometryCollection.class, new GeoShapeGeometryCollectionDeserializer());
	}


	private static class GeoShapeGeometryCollectionDeserializer extends JsonDeserializer<GeoShapeGeometryCollection> {

		@Override
		public GeoShapeGeometryCollection deserialize(JsonParser p, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {

			ObjectMapper om = new ObjectMapper();
			om.registerModule(new GeoShapeModule());

			JsonNode node = p.readValueAsTree();
			JsonNode type = node.get("type");
			ArrayNode coordinates = (ArrayNode) node.get("geometries");

			List<GeoShape<?>> shapes = new ArrayList<>();
			for(JsonNode coordinate : coordinates) {
				shapes.add(om.treeToValue(coordinate, GeoShape.class));
			}

			return new GeoShapeGeometryCollection(shapes);
		}

	}


	private static class GeoShapeGeometryCollectionSerializer extends JsonSerializer<GeoShapeGeometryCollection> {

		@Override
		public void serialize(GeoShapeGeometryCollection value, JsonGenerator gen, SerializerProvider serializers)
				throws IOException, JsonProcessingException {

			gen.writeStartObject();
			gen.writeObjectField("type", value.getType());
			gen.writeObjectField("geometries", value.getCoordinates());
			gen.writeEndObject();
		}

	}


	private static abstract class GeoShapeDeserializer<T extends GeoShape<?>> extends JsonDeserializer<T> {

		/*
		 * (non-Javadoc)
		 * @see com.fasterxml.jackson.databind.JsonDeserializer#deserialize(com.fasterxml.jackson.core.JsonParser, com.fasterxml.jackson.databind.DeserializationContext)
		 */
		@Override
		public T deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {

			JsonNode node = jp.readValueAsTree();
			JsonNode coordinates = node.get("coordinates");

			if (coordinates != null && coordinates.isArray()) {
				return doDeserialize((ArrayNode) coordinates);
			}
			return null;
		}

		/**
		 * Perform the actual deserialization given the {@literal coordinates} as {@link ArrayNode}.
		 *
		 * @param coordinates
		 * @return
		 */
		protected abstract T doDeserialize(ArrayNode coordinates);

		/**
		 * Get the {@link Coordinate} representation of given {@link ArrayNode} assuming {@code node.[0]} represents
		 * {@literal x - coordinate} and {@code node.[1]} is {@literal y}.
		 *
		 * @param node can be {@literal null}.
		 * @return {@literal null} when given a {@code null} value.
		 */
		protected Coordinate toCoordinate(ArrayNode node) {

			if (node == null) {
				return null;
			}

			return new Coordinate(node.get(0).asDouble(), node.get(1).asDouble());
		}

		/**
		 * Get the {@link Point} representation of given {@link ArrayNode} assuming {@code node.[0]} represents
		 * {@literal x - coordinate} and {@code node.[1]} is {@literal y}.
		 *
		 * @param node can be {@literal null}.
		 * @return {@literal null} when given a {@code null} value.
		 */
		protected Point toPoint(ArrayNode node) {

			if (node == null) {
				return null;
			}

			return new Point(node.get(0).asDouble(), node.get(1).asDouble());
		}

		/**
		 * Get the points nested within given {@link ArrayNode}.
		 *
		 * @param node can be {@literal null}.
		 * @return {@literal empty list} when given a {@code null} value.
		 */
		protected List<Point> toPoints(ArrayNode node) {

			if (node == null) {
				return Collections.emptyList();
			}

			List<Point> points = new ArrayList<Point>(node.size());

			for (JsonNode coordinatePair : node) {
				if (coordinatePair.isArray()) {
					points.add(toPoint((ArrayNode) coordinatePair));
				}
			}
			return points;
		}

		protected GeoShapePolygon toPolygon(ArrayNode node) {
			GeoShapePolygon polygon = null;
			for (JsonNode ring : node) {
				if(polygon == null) {
					polygon = new GeoShapePolygon(toPoints((ArrayNode) ring));
					continue;
				}
				polygon = polygon.addHole(toPoints((ArrayNode) ring));
			}
			return polygon;
		}

		protected GeoShapeLinestring toLinestring(ArrayNode node) {
			return new GeoShapeLinestring(toPoints(node));
		}

	}


	private static class GeoShapePolygonDeserializer extends GeoShapeDeserializer<GeoShapePolygon> {

		@Override
		protected GeoShapePolygon doDeserialize(ArrayNode coordinates) {
			return toPolygon(coordinates);
		}
	}


	private static class GeoShapeMultiPolygonDeserializer extends GeoShapeDeserializer<GeoShapeMultiPolygon> {

		@Override
		protected GeoShapeMultiPolygon doDeserialize(ArrayNode coordinates) {
			List<GeoShapePolygon> polygons = new ArrayList<>(coordinates.size());
			for (JsonNode ring : coordinates) {
				polygons.add(toPolygon((ArrayNode) ring));
			}
			return new GeoShapeMultiPolygon(polygons);
		}
	}


	private static class GeoShapeMultiLinestringDeserializer extends GeoShapeDeserializer<GeoShapeMultiLinestring> {

		@Override
		protected GeoShapeMultiLinestring doDeserialize(ArrayNode coordinates) {
			List<GeoShapeLinestring> linestrings = new ArrayList<>(coordinates.size());
			for (JsonNode ring : coordinates) {
				linestrings.add(toLinestring((ArrayNode) ring));
			}
			return new GeoShapeMultiLinestring(linestrings);
		}
	}


	private static class PolygonSerializer extends JsonSerializer<Polygon> {
		@Override
		public void serialize(Polygon value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
			gen.writeObject(GeoShapePolygon.fromShape(value));
		}
	}


	private static class PolygonDeserializer extends JsonDeserializer<Polygon> {
		@Override
		public Polygon deserialize(JsonParser p, DeserializationContext context) throws IOException {
			return GeoShapePolygon.toShape(p.readValueAs(GeoShapePolygon.class));
		}
	}


	private static class CoordinateSerializer extends JsonSerializer<Coordinate> {
		@Override
		public void serialize(Coordinate value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
			gen.writeArray(value.getCoordinate(), 0, 2);
		}
	}


	private static class CoordinateDeerializer extends JsonDeserializer<Coordinate> {
		@Override
		public Coordinate deserialize(JsonParser p, DeserializationContext context) throws IOException {
			double [] coordinate = p.readValueAs(double[].class);
			return new Coordinate(coordinate[0], coordinate[1]);
		}
	}


	private static class GeoShapeTypeSerializer extends JsonSerializer<GeoShapeType> {
		@Override
		public void serialize(GeoShapeType value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
			gen.writeString(value.toString());
		}
	}


	class GeoShapeTypeDeserializer extends JsonDeserializer<GeoShapeType> {
		@Override
		public GeoShapeType deserialize(JsonParser p, DeserializationContext context) throws IOException {
			return GeoShapeType.getEnum(p.readValueAs(String.class));
		}
	}


	private static class TypeGeoShapeDeserializer extends JsonDeserializer<GeoShape> {
		@SuppressWarnings("unchecked")
		@Override
		public GeoShape deserialize(JsonParser p, DeserializationContext context) throws IOException {
			ObjectMapper om = new ObjectMapper();
			om.registerModule(new GeoShapeModule());
			JsonNode node = om.readTree(p);
			GeoShapeType type = GeoShapeType.getEnum(node.path("type").asText());
			Class targetClass = null;
			switch(type) {
			case POLYGON:
				targetClass = GeoShapePolygon.class;
				break;
			case LINESTRING:
				targetClass = GeoShapeLinestring.class;
				break;
			case POINT:
				targetClass = GeoShapePoint.class;
				break;
			case MULTIPOINT:
				targetClass = GeoShapeMultiPoint.class;
				break;
			case MULTIPOLYGON:
				targetClass = GeoShapeMultiPolygon.class;
				break;
			case MULTILINESTRING:
				targetClass = GeoShapeMultiLinestring.class;
				break;
			case GEOMETRY_COLLECTION:
				targetClass = GeoShapeGeometryCollection.class;
				break;
			}

			Assert.notNull(targetClass, "Unknown GeoShape type.");

			return (GeoShape<?>) om.treeToValue(node, targetClass);
		}
	}


	private static class GeoShapeSerializer<T extends GeoShape<?>> extends JsonSerializer<T> {
		@Override
		public void serialize(T value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
			gen.writeStartObject();
			gen.writeObjectField("type", value.getType());
			gen.writeObjectField("coordinates", finalCoordinates(value.getCoordinates()));
			gen.writeEndObject();
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		private Object finalCoordinates(Object coordinates) {
			if(coordinates instanceof GeoShape) {
				return finalCoordinates(((GeoShape) coordinates).getCoordinates());
			}
			if(coordinates instanceof Collection) {
				@SuppressWarnings("rawtypes")
				List list = new ArrayList();
				for(Object o : (Collection) coordinates) {
					list.add(finalCoordinates(o));
				}

				return list;
			}
			return coordinates;
		}
	}

}