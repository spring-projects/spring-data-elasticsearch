/*
 * Copyright 2014-2019 the original author or authors.
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
 * @author Jakub Vavrik
 *         Values based on reference doc - https://www.elastic.co/guide/reference/mapping/date-format/
 */
public enum DateFormat {
	none, custom, basic_date, basic_date_time, basic_date_time_no_millis, basic_ordinal_date, basic_ordinal_date_time,
	basic_ordinal_date_time_no_millis, basic_time, basic_time_no_millis, basic_t_time, basic_t_time_no_millis,
	basic_week_date, basic_week_date_time, basic_week_date_time_no_millis, date, date_hour, date_hour_minute,
	date_hour_minute_second, date_hour_minute_second_fraction, date_hour_minute_second_millis, date_optional_time,
	date_time, date_time_no_millis, hour, hour_minute, hour_minute_second, hour_minute_second_fraction,
	hour_minute_second_millis, ordinal_date, ordinal_date_time, ordinal_date_time_no_millis, time, time_no_millis,
	t_time, t_time_no_millis, week_date, week_date_time, weekDateTimeNoMillis, week_year, weekyearWeek,
	weekyearWeekDay, year, year_month, year_month_day
}
