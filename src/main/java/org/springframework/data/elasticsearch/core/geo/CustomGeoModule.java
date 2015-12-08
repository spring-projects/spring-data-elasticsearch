package org.springframework.data.elasticsearch.core.geo;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.data.geo.Point;

/**
 * @author Artur Konaczak
 */
public class CustomGeoModule extends SimpleModule {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new {@link org.springframework.data.elasticsearch.core.geo.CustomGeoModule} registering serializers and deserializers for spring data commons geo-spatial types.
	 */
	public CustomGeoModule() {

		super("Spring Data Elasticsearch Geo", new Version(1, 0, 0, null, "org.springframework.data.elasticsearch", "spring-data-elasticsearch-geo"));
		addSerializer(Point.class, new PointSerializer());
		addDeserializer(Point.class, new PointDeserializer());
	}
}

class PointSerializer extends JsonSerializer<Point> {

	@Override
	public void serialize(Point value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
		gen.writeObject(GeoPoint.fromPoint(value));
//		gen.writeStartObject();
//		gen.writeNumberField("lat", value.getY());
//		gen.writeNumberField("lon", value.getX());
//		gen.writeEndObject();
	}
}

class PointDeserializer extends JsonDeserializer<Point> {

	@Override
	public Point deserialize(JsonParser p, DeserializationContext context) throws IOException, JsonProcessingException {

		GeoPoint point = p.readValueAs(GeoPoint.class);
//		Double lat = null;
//		Double lon = null;
//		//skipp field name
//		p.nextFieldName();
//		if ("lat".equals(p.getCurrentName())) {
//			//get value
//			p.nextFieldName();
//			lat = p.getDoubleValue();
//			p.nextFieldName();
//		}
//		if ("lon".equals(p.getCurrentName())) {
//			//get value
//			p.nextFieldName();
//			lon = p.getDoubleValue();
//		}
//		return new Point(lon, lat);
		return GeoPoint.toPoint(point);
	}
}
