/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core.geo;

import java.util.List;

/**
 * Geo bbox used for #{@link org.springframework.data.elasticsearch.core.query.Criteria}.
 *
 * @author Franck Marchand
 */
public class GeoBBox {
    private GeoLocation topLeft;
    private GeoLocation bottomRight;

    public GeoBBox(GeoLocation topLeft, GeoLocation bottomRight) {
        this.topLeft = topLeft;
        this.bottomRight = bottomRight;
    }

    public GeoLocation getTopLeft() {
        return topLeft;
    }

    public void setTopLeft(GeoLocation topLeft) {
        this.topLeft = topLeft;
    }

    public GeoLocation getBottomRight() {
        return bottomRight;
    }

    public void setBottomRight(GeoLocation bottomRight) {
        this.bottomRight = bottomRight;
    }
}
