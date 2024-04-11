/*
 * Copyright 2022-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.utils.geohash;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Utility class for converting to and from WKT
 */
public class WellKnownText {
	/* The instance of WKT serializer that coerces values and accepts Z component */
	public static final WellKnownText INSTANCE = new WellKnownText(true, new StandardValidator(true));

	public static final String EMPTY = "EMPTY";
	public static final String SPACE = " ";
	public static final String LPAREN = "(";
	public static final String RPAREN = ")";
	public static final String COMMA = ",";
	public static final String NAN = "NaN";

	private static final String NUMBER = "<NUMBER>";
	private static final String EOF = "END-OF-STREAM";
	private static final String EOL = "END-OF-LINE";

	@SuppressWarnings("FieldCanBeLocal") private final boolean coerce;
	private final GeometryValidator validator;

	public WellKnownText(boolean coerce, GeometryValidator validator) {
		this.coerce = coerce;
		this.validator = validator;
	}

	public String toWKT(Geometry geometry) {
		StringBuilder builder = new StringBuilder();
		toWKT(geometry, builder);
		return builder.toString();
	}

	public void toWKT(Geometry geometry, StringBuilder sb) {
		sb.append(getWKTName(geometry));
		sb.append(SPACE);
		if (geometry.isEmpty()) {
			sb.append(EMPTY);
		} else {
			geometry.visit(new GeometryVisitor<Void, RuntimeException>() {

				@Override
				public Void visit(Point point) {
					if (point.isEmpty()) {
						sb.append(EMPTY);
					} else {
						sb.append(LPAREN);
						visitPoint(point.getX(), point.getY(), point.getZ());
						sb.append(RPAREN);
					}
					return null;
				}

				private void visitPoint(double lon, double lat, double alt) {
					sb.append(lon).append(SPACE).append(lat);
					if (!Double.isNaN(alt)) {
						sb.append(SPACE).append(alt);
					}
				}

				@Override
				public Void visit(Rectangle rectangle) {
					sb.append(LPAREN);
					// minX, maxX, maxY, minY
					sb.append(rectangle.getMinX());
					sb.append(COMMA);
					sb.append(SPACE);
					sb.append(rectangle.getMaxX());
					sb.append(COMMA);
					sb.append(SPACE);
					sb.append(rectangle.getMaxY());
					sb.append(COMMA);
					sb.append(SPACE);
					sb.append(rectangle.getMinY());
					if (rectangle.hasZ()) {
						sb.append(COMMA);
						sb.append(SPACE);
						sb.append(rectangle.getMinZ());
						sb.append(COMMA);
						sb.append(SPACE);
						sb.append(rectangle.getMaxZ());
					}
					sb.append(RPAREN);
					return null;
				}
			});
		}
	}

	public Geometry fromWKT(String wkt) throws IOException, ParseException {
		try (StringReader reader = new StringReader(wkt)) {
			// setup the tokenizer; configured to read words w/o numbers
			StreamTokenizer tokenizer = new StreamTokenizer(reader);
			tokenizer.resetSyntax();
			tokenizer.wordChars('a', 'z');
			tokenizer.wordChars('A', 'Z');
			tokenizer.wordChars(128 + 32, 255);
			tokenizer.wordChars('0', '9');
			tokenizer.wordChars('-', '-');
			tokenizer.wordChars('+', '+');
			tokenizer.wordChars('.', '.');
			tokenizer.whitespaceChars(' ', ' ');
			tokenizer.whitespaceChars('\t', '\t');
			tokenizer.whitespaceChars('\r', '\r');
			tokenizer.whitespaceChars('\n', '\n');
			tokenizer.commentChar('#');
			Geometry geometry = parseGeometry(tokenizer);
			validator.validate(geometry);
			return geometry;
		}
	}

	/**
	 * parse geometry from the stream tokenizer
	 */
	private Geometry parseGeometry(StreamTokenizer stream) throws IOException, ParseException {
		final String type = nextWord(stream).toLowerCase(Locale.ROOT);
		return switch (type) {
			case "point" -> parsePoint(stream);
			case "bbox" -> parseBBox(stream);
			default -> throw new IllegalArgumentException("Unknown geometry type: " + type);
		};
	}

	private Point parsePoint(StreamTokenizer stream) throws IOException, ParseException {
		if (nextEmptyOrOpen(stream).equals(EMPTY)) {
			return Point.EMPTY;
		}
		double lon = nextNumber(stream);
		double lat = nextNumber(stream);
		Point pt;
		if (isNumberNext(stream)) {
			pt = new Point(lon, lat, nextNumber(stream));
		} else {
			pt = new Point(lon, lat);
		}
		nextCloser(stream);
		return pt;
	}

	private void parseCoordinates(StreamTokenizer stream, ArrayList<Double> lats, ArrayList<Double> lons,
			ArrayList<Double> alts) throws IOException, ParseException {
		parseCoordinate(stream, lats, lons, alts);
		while (nextCloserOrComma(stream).equals(COMMA)) {
			parseCoordinate(stream, lats, lons, alts);
		}
	}

	private void parseCoordinate(StreamTokenizer stream, ArrayList<Double> lats, ArrayList<Double> lons,
			ArrayList<Double> alts) throws IOException, ParseException {
		lons.add(nextNumber(stream));
		lats.add(nextNumber(stream));
		if (isNumberNext(stream)) {
			alts.add(nextNumber(stream));
		}
		if (!alts.isEmpty() && alts.size() != lons.size()) {
			throw new ParseException("coordinate dimensions do not match: " + tokenString(stream), stream.lineno());
		}
	}

	private Rectangle parseBBox(StreamTokenizer stream) throws IOException, ParseException {
		if (nextEmptyOrOpen(stream).equals(EMPTY)) {
			return Rectangle.EMPTY;
		}
		double minLon = nextNumber(stream);
		nextComma(stream);
		double maxLon = nextNumber(stream);
		nextComma(stream);
		double maxLat = nextNumber(stream);
		nextComma(stream);
		double minLat = nextNumber(stream);
		nextCloser(stream);
		return new Rectangle(minLon, maxLon, maxLat, minLat);
	}

	/**
	 * next word in the stream
	 */
	private String nextWord(StreamTokenizer stream) throws ParseException, IOException {
		return switch (stream.nextToken()) {
			case StreamTokenizer.TT_WORD -> {
				final String word = stream.sval;
				yield word.equalsIgnoreCase(EMPTY) ? EMPTY : word;
			}
			case '(' -> LPAREN;
			case ')' -> RPAREN;
			case ',' -> COMMA;
			default -> throw new ParseException("expected word but found: " + tokenString(stream), stream.lineno());
		};
	}

	private double nextNumber(StreamTokenizer stream) throws IOException, ParseException {
		if (stream.nextToken() == StreamTokenizer.TT_WORD) {
			if (stream.sval.equalsIgnoreCase(NAN)) {
				return Double.NaN;
			} else {
				try {
					return Double.parseDouble(stream.sval);
				} catch (NumberFormatException e) {
					throw new ParseException("invalid number found: " + stream.sval, stream.lineno());
				}
			}
		}
		throw new ParseException("expected number but found: " + tokenString(stream), stream.lineno());
	}

	private String tokenString(StreamTokenizer stream) {
		return switch (stream.ttype) {
			case StreamTokenizer.TT_WORD -> stream.sval;
			case StreamTokenizer.TT_EOF -> EOF;
			case StreamTokenizer.TT_EOL -> EOL;
			case StreamTokenizer.TT_NUMBER -> NUMBER;
			default -> "'" + (char) stream.ttype + '\'';
		};
	}

	private boolean isNumberNext(StreamTokenizer stream) throws IOException {
		final int type = stream.nextToken();
		stream.pushBack();
		return type == StreamTokenizer.TT_WORD;
	}

	private String nextEmptyOrOpen(StreamTokenizer stream) throws IOException, ParseException {
		final String next = nextWord(stream);
		if (next.equals(EMPTY) || next.equals(LPAREN)) {
			return next;
		}
		throw new ParseException("expected " + EMPTY + " or " + LPAREN + " but found: " + tokenString(stream),
				stream.lineno());
	}

	private String nextCloser(StreamTokenizer stream) throws IOException, ParseException {
		if (nextWord(stream).equals(RPAREN)) {
			return RPAREN;
		}
		throw new ParseException("expected " + RPAREN + " but found: " + tokenString(stream), stream.lineno());
	}

	private String nextComma(StreamTokenizer stream) throws IOException, ParseException {
		if (nextWord(stream).equals(COMMA)) {
			return COMMA;
		}
		throw new ParseException("expected " + COMMA + " but found: " + tokenString(stream), stream.lineno());
	}

	private String nextOpener(StreamTokenizer stream) throws IOException, ParseException {
		if (nextWord(stream).equals(LPAREN)) {
			return LPAREN;
		}
		throw new ParseException("expected " + LPAREN + " but found: " + tokenString(stream), stream.lineno());
	}

	private String nextCloserOrComma(StreamTokenizer stream) throws IOException, ParseException {
		String token = nextWord(stream);
		if (token.equals(COMMA) || token.equals(RPAREN)) {
			return token;
		}
		throw new ParseException("expected " + COMMA + " or " + RPAREN + " but found: " + tokenString(stream),
				stream.lineno());
	}

	@SuppressWarnings("Convert2Diamond")
	private static String getWKTName(Geometry geometry) {
		return geometry.visit(new GeometryVisitor<String, RuntimeException>() {

			@Override
			public String visit(Point point) {
				return "POINT";
			}

			@Override
			public String visit(Rectangle rectangle) {
				return "BBOX";
			}
		});
	}
}
