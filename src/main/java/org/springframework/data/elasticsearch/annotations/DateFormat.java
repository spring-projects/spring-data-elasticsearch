/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
 *         Values based on reference doc - http://www.elasticsearch.org/guide/reference/mapping/date-format/
 */
public enum DateFormat {
	none, custom, basic_date, basic_date_time, basic_date_time_no_millis, basic_ordinal_date, basic_ordinal_date_time,
	basic_ordinal_date_time_no_millis, basic_time, basic_time_no_millis, basic_t_time, basic_t_time_no_millis,
	basic_week_date, basic_week_date_time, basic_week_date_time_no_millis, date, date_hour, date_hour_minute,
	date_hour_minute_second, date_hour_minute_second_fraction, date_hour_minute_second_millis, date_optional_time,
	date_time, date_time_no_millis, hour, hour_minute, hour_minute_second, hour_minute_second_fraction,
	hour_minute_second_millis, ordinal_date, ordinal_date_time, ordinal_date_time_no_millis, time, time_no_millis,
	t_time, t_time_no_millis, week_date, week_date_time, weekDateTimeNoMillis, week_year, weekyearWeek,
	weekyearWeekDay, year, year_month, year_month_day,
	//Added new formats from Elastic v2.4 due to DATAES-287
	strict_year_month_day, strict_year_month, strict_weekyear_week_day, strict_weekyear_week, strict_weekyear,
	strict_week_date_time_no_millis, strict_week_date_time, strict_week_date, strict_t_time_no_millis, strict_t_time,
	strict_time_no_millis, strict_time, strict_ordinal_date_time_no_millis, strict_ordinal_date_time,
	strict_hour_minute_second_millis, strict_hour_minute_second_fraction, strict_hour_minute_second,
	strict_hour_minute, strict_hour, strict_date_time_no_millis, strict_date_time,
	strict_date_hour_minute_second_millis, strict_date_hour_minute_second_fraction, strict_date_hour_minute_second,
	strict_date_hour_minute, strict_date_hour, strict_date, strict_basic_week_date_time_no_millis,
	strict_basic_week_date_time, strict_basic_week_date, strict_date_optional_time, epoch_millis, epoch_seconds
}
