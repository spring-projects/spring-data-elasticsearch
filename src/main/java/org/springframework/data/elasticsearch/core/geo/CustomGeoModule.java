package org.springframework.data.elasticsearch.core.geo;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;

import org.springframework.data.geo.Point;

class PointSerializer extends ValueSerializer<Point> {
	@Override
	public void serialize(Point value, JsonGenerator gen, SerializationContext serializers) throws JacksonException {
		gen.writePOJO(GeoPoint.fromPoint(value));
	}
}

class PointDeserializer extends ValueDeserializer<Point> {
	@Override
	public Point deserialize(JsonParser p, DeserializationContext context) throws JacksonException {
		return GeoPoint.toPoint(p.readValueAs(GeoPoint.class));
	}
}
