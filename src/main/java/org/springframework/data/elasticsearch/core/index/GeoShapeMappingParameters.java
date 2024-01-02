/*
 * Copyright 2020-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.core.index;

import java.io.IOException;

import org.springframework.data.elasticsearch.annotations.GeoShapeField;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Peter-Josef Meisch
 */
final class GeoShapeMappingParameters {
	private static final String FIELD_PARAM_TYPE = "type";
	private static final String FIELD_PARAM_COERCE = "coerce";
	private static final String FIELD_PARAM_IGNORE_MALFORMED = "ignore_malformed";
	private static final String FIELD_PARAM_IGNORE_Z_VALUE = "ignore_z_value";
	private static final String FIELD_PARAM_ORIENTATION = "orientation";

	private static final String TYPE_VALUE_GEO_SHAPE = "geo_shape";

	private final boolean coerce;
	private final boolean ignoreMalformed;
	private final boolean ignoreZValue;
	private final GeoShapeField.Orientation orientation;

	/**
	 * Creates a GeoShapeMappingParameters from the given annotation.
	 *
	 * @param annotation if null, default values are set in the returned object
	 * @return a parameters object
	 */
	public static GeoShapeMappingParameters from(@Nullable GeoShapeField annotation) {

		if (annotation == null) {
			return new GeoShapeMappingParameters(false, false, true, GeoShapeField.Orientation.ccw);
		} else {
			return new GeoShapeMappingParameters(annotation.coerce(), annotation.ignoreMalformed(), annotation.ignoreZValue(),
					annotation.orientation());
		}
	}

	private GeoShapeMappingParameters(boolean coerce, boolean ignoreMalformed, boolean ignoreZValue,
			GeoShapeField.Orientation orientation) {
		this.coerce = coerce;
		this.ignoreMalformed = ignoreMalformed;
		this.ignoreZValue = ignoreZValue;
		this.orientation = orientation;
	}

	public boolean isCoerce() {
		return coerce;
	}

	public boolean isIgnoreMalformed() {
		return ignoreMalformed;
	}

	public boolean isIgnoreZValue() {
		return ignoreZValue;
	}

	public GeoShapeField.Orientation getOrientation() {
		return orientation;
	}

	public void writeTypeAndParametersTo(ObjectNode objectNode) throws IOException {

		Assert.notNull(objectNode, "objectNode must not be null");

		if (coerce) {
			objectNode.put(FIELD_PARAM_COERCE, coerce);
		}

		if (ignoreMalformed) {
			objectNode.put(FIELD_PARAM_IGNORE_MALFORMED, ignoreMalformed);
		}

		if (!ignoreZValue) {
			objectNode.put(FIELD_PARAM_IGNORE_Z_VALUE, ignoreZValue);
		}

		if (orientation != GeoShapeField.Orientation.ccw) {
			objectNode.put(FIELD_PARAM_ORIENTATION, orientation.name());
		}

		objectNode.put(FIELD_PARAM_TYPE, TYPE_VALUE_GEO_SHAPE);
	}
}
