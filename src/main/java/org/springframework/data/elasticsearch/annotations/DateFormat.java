package org.springframework.data.elasticsearch.annotations;

/**
 * @author Jakub Vavrik
 *
 * Values based on reference doc - http://www.elasticsearch.org/guide/reference/mapping/date-format/
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
