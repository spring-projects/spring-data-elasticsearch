package org.springframework.data.elasticsearch.core.geo;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.data.geo.Point;

class PointSerializer extends JsonSerializer<Point> {
	@Override
	public void serialize(Point value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		gen.writeObject(GeoPoint.fromPoint(value));
	}
}

class PointDeserializer extends JsonDeserializer<Point> {
	@Override
	public Point deserialize(JsonParser p, DeserializationContext context) throws IOException {
		return GeoPoint.toPoint(p.readValueAs(GeoPoint.class));
	}
}
