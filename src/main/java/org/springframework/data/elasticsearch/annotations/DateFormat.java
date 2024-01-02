/*
 * Copyright 2014-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.annotations;

/**
 * Values based on <a href="https://www.elastic.co/guide/reference/mapping/date-format/">Elasticsearch reference
 * documentation</a>. The patterns are taken from this documentation and slightly adapted so that a Java
 * {@link java.time.format.DateTimeFormatter} produces the same values as the Elasticsearch formatter. Use
 * <code>format = {}</code> to disable built-in date formats in the {@link Field} annotation. If you want to use only a
 * custom date format pattern, you must set the <code>format</code> property to empty <code>{}</code>.
 *
 * @author Jakub Vavrik
 * @author Tim te Beek
 * @author Peter-Josef Meisch
 * @author Sascha Woo
 */
public enum DateFormat {
	basic_date("uuuuMMdd"), //
	basic_date_time("uuuuMMdd'T'HHmmss.SSSXXX"), //
	basic_date_time_no_millis("uuuuMMdd'T'HHmmssXXX"), //
	basic_ordinal_date("uuuuDDD"), //
	basic_ordinal_date_time("yyyyDDD'T'HHmmss.SSSXXX"), //
	basic_ordinal_date_time_no_millis("yyyyDDD'T'HHmmssXXX"), //
	basic_time("HHmmss.SSSXXX"), //
	basic_time_no_millis("HHmmssXXX"), //
	basic_t_time("'T'HHmmss.SSSXXX"), //
	basic_t_time_no_millis("'T'HHmmssXXX"), //
	basic_week_date("YYYY'W'wwe"), // week-based-year!
	/**
	 * @since 5.3
	 */
	strict_basic_week_date("YYYY'W'wwe"), // week-based-year!
	basic_week_date_time("YYYY'W'wwe'T'HHmmss.SSSX"), // here Elasticsearch uses a different zone format
	/**
	 * @since 5.3
	 */
	strict_basic_week_date_time("YYYY'W'wwe'T'HHmmss.SSSX"), // here Elasticsearch uses a different zone format
	basic_week_date_time_no_millis("YYYY'W'wwe'T'HHmmssX"), //
	/**
	 * @since 5.3
	 */
	strict_basic_week_date_time_no_millis("YYYY'W'wwe'T'HHmmssX"), //
	date("uuuu-MM-dd"), //
	/**
	 * @since 5.3
	 */
	strict_date("uuuu-MM-dd"), //
	date_hour("uuuu-MM-dd'T'HH"), //
	/**
	 * @since 5.3
	 */
	strict_date_hour("uuuu-MM-dd'T'HH"), //
	date_hour_minute("uuuu-MM-dd'T'HH:mm"), //
	/**
	 * @since 5.3
	 */
	strict_date_hour_minute("uuuu-MM-dd'T'HH:mm"), //
	date_hour_minute_second("uuuu-MM-dd'T'HH:mm:ss"), //
	/**
	 * @since 5.3
	 */
	strict_date_hour_minute_second("uuuu-MM-dd'T'HH:mm:ss"), //
	date_hour_minute_second_fraction("uuuu-MM-dd'T'HH:mm:ss.SSS"), //
	/**
	 * @since 5.3
	 */
	strict_date_hour_minute_second_fraction("uuuu-MM-dd'T'HH:mm:ss.SSS"), //
	date_hour_minute_second_millis("uuuu-MM-dd'T'HH:mm:ss.SSS"), //
	/**
	 * @since 5.3
	 */
	strict_date_hour_minute_second_millis("uuuu-MM-dd'T'HH:mm:ss.SSS"), //
	date_optional_time("uuuu-MM-dd['T'HH:mm:ss.SSSXXX]"), //
	/**
	 * @since 5.3
	 */
	strict_date_optional_time("uuuu-MM-dd['T'HH:mm:ss.SSSXXX]"), //
	strict_date_optional_time_nanos("uuuu-MM-dd['T'HH:mm:ss.SSSSSSXXX]"), //
	date_time("uuuu-MM-dd'T'HH:mm:ss.SSSXXX"), //
	/**
	 * @since 5.3
	 */
	strict_date_time("uuuu-MM-dd'T'HH:mm:ss.SSSXXX"), //
	date_time_no_millis("uuuu-MM-dd'T'HH:mm:ssVV"), // here Elasticsearch uses the zone-id in its implementation
	/**
	 * @since 5.3
	 */
	strict_date_time_no_millis("uuuu-MM-dd'T'HH:mm:ssVV"), // here Elasticsearch uses the zone-id in its implementation
	epoch_millis("epoch_millis"), //
	epoch_second("epoch_second"), //
	hour("HH"), //
	/**
	 * @since 5.3
	 */
	strict_hour("HH"), //
	hour_minute("HH:mm"), //
	/**
	 * @since 5.3
	 */
	strict_hour_minute("HH:mm"), //
	hour_minute_second("HH:mm:ss"), //
	/**
	 * @since 5.3
	 */
	strict_hour_minute_second("HH:mm:ss"), //
	hour_minute_second_fraction("HH:mm:ss.SSS"), //
	/**
	 * @since 5.3
	 */
	strict_hour_minute_second_fraction("HH:mm:ss.SSS"), //
	hour_minute_second_millis("HH:mm:ss.SSS"), //
	/**
	 * @since 5.3
	 */
	strict_hour_minute_second_millis("HH:mm:ss.SSS"), //
	ordinal_date("uuuu-DDD"), //
	/**
	 * @since 5.3
	 */
	strict_ordinal_date("uuuu-DDD"), //
	ordinal_date_time("uuuu-DDD'T'HH:mm:ss.SSSXXX"), //
	/**
	 * @since 5.3
	 */
	strict_ordinal_date_time("uuuu-DDD'T'HH:mm:ss.SSSXXX"), //
	ordinal_date_time_no_millis("uuuu-DDD'T'HH:mm:ssXXX"), //
	/**
	 * @since 5.3
	 */
	strict_ordinal_date_time_no_millis("uuuu-DDD'T'HH:mm:ssXXX"), //
	time("HH:mm:ss.SSSXXX"), //
	/**
	 * @since 5.3
	 */
	strict_time("HH:mm:ss.SSSXXX"), //
	time_no_millis("HH:mm:ssXXX"), //
	/**
	 * @since 5.3
	 */
	strict_time_no_millis("HH:mm:ssXXX"), //
	t_time("'T'HH:mm:ss.SSSXXX"), //
	/**
	 * @since 5.3
	 */
	strict_t_time("'T'HH:mm:ss.SSSXXX"), //
	t_time_no_millis("'T'HH:mm:ssXXX"), //
	/**
	 * @since 5.3
	 */
	strict_t_time_no_millis("'T'HH:mm:ssXXX"), //
	week_date("YYYY-'W'ww-e"), //
	/**
	 * @since 5.3
	 */
	strict_week_date("YYYY-'W'ww-e"), //
	week_date_time("YYYY-'W'ww-e'T'HH:mm:ss.SSSXXX"), //
	/**
	 * @since 5.3
	 */
	strict_week_date_time("YYYY-'W'ww-e'T'HH:mm:ss.SSSXXX"), //
	week_date_time_no_millis("YYYY-'W'ww-e'T'HH:mm:ssXXX"), //
	/**
	 * @since 5.3
	 */
	strict_week_date_time_no_millis("YYYY-'W'ww-e'T'HH:mm:ssXXX"), //
	weekyear(""), // no TemporalAccessor available for these 3
	/**
	 * @since 5.3
	 */
	strict_weekyear(""), // no TemporalAccessor available for these 3
	weekyear_week(""), //
	/**
	 * @since 5.3
	 */
	strict_weekyear_week(""), //
	weekyear_week_day(""), //
	/**
	 * @since 5.3
	 */
	strict_strict_weekyear_week_day(""), //
	year("uuuu"), //
	/**
	 * @since 5.3
	 */
	strict_year("uuuu"), //
	year_month("uuuu-MM"), //
	/**
	 * @since 5.3
	 */
	strict_year_month("uuuu-MM"), //
	year_month_day("uuuu-MM-dd"), //
	/**
	 * @since 5.3
	 */
	strict_year_month_day("uuuu-MM-dd"); //

	private final String pattern;

	DateFormat(String pattern) {
		this.pattern = pattern;
	}

	/**
	 * @since 4.2
	 */
	public String getPattern() {
		return pattern;
	}
}
