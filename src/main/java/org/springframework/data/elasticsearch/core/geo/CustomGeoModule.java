package org.springframework.data.elasticsearch.core.geo;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;

import org.jspecify.annotations.Nullable;
import org.springframework.data.geo.Point;
import org.springframework.util.Assert;

class PointSerializer extends ValueSerializer<Point> {
	@Override
	public void serialize(Point value, @Nullable JsonGenerator gen, @Nullable SerializationContext serializers)
			throws JacksonException {

		Assert.notNull(gen, "gen must not be null");

		gen.writePOJO(GeoPoint.fromPoint(value));
	}
}

class PointDeserializer extends ValueDeserializer<Point> {
	@Override
	public Point deserialize(@Nullable JsonParser p, @Nullable DeserializationContext context) throws JacksonException {

		Assert.notNull(p, "p must not be null");

		return GeoPoint.toPoint(p.readValueAs(GeoPoint.class));
	}
}
