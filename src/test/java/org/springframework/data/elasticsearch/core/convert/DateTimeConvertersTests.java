/*
 * Copyright 2013-2019 the original author or authors.
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
package org.springframework.data.elasticsearch.core.convert;

import static org.assertj.core.api.Assertions.*;

import java.util.Calendar;
import java.util.TimeZone;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 */
public class DateTimeConvertersTests {

	@Test
	public void testJodaDateTimeConverterWithNullValue() {
		assertThat(DateTimeConverters.JodaDateTimeConverter.INSTANCE.convert(null)).isNull();
	}

	@Test
	public void testJodaDateTimeConverter() {
		DateTime dateTime = new DateTime(2013, 1, 24, 6, 35, 0, DateTimeZone.UTC);
		assertThat(DateTimeConverters.JodaDateTimeConverter.INSTANCE.convert(dateTime))
				.isEqualTo("2013-01-24T06:35:00.000Z");
	}

	@Test
	public void testJodaLocalDateTimeConverterWithNullValue() {
		assertThat(DateTimeConverters.JodaLocalDateTimeConverter.INSTANCE.convert(null)).isNull();
	}

	@Test
	public void testJodaLocalDateTimeConverter() {
		LocalDateTime dateTime = new LocalDateTime(new DateTime(2013, 1, 24, 6, 35, 0, DateTimeZone.UTC).getMillis(),
				DateTimeZone.UTC);
		assertThat(DateTimeConverters.JodaLocalDateTimeConverter.INSTANCE.convert(dateTime))
				.isEqualTo("2013-01-24T06:35:00.000Z");
	}

	@Test
	public void testJavaDateConverterWithNullValue() {
		assertThat(DateTimeConverters.JavaDateConverter.INSTANCE.convert(null)).isNull();
	}

	@Test
	public void testJavaDateConverter() {
		DateTime dateTime = new DateTime(2013, 1, 24, 6, 35, 0, DateTimeZone.UTC);
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
		calendar.setTimeInMillis(dateTime.getMillis());

		assertThat(DateTimeConverters.JavaDateConverter.INSTANCE.convert(calendar.getTime()))
				.isEqualTo("2013-01-24T06:35:00.000Z");
	}
}
