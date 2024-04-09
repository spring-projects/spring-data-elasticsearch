package org.springframework.data.elasticsearch.core.convert;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.data.elasticsearch.annotations.DateFormat;

/**
 * @author Peter-Josef Meisch
 * @author Tim te Beek
 * @author Sascha Woo
 */
class ElasticsearchDateConverterUnitTests {

	private final ZonedDateTime zdt = ZonedDateTime.now(ZoneId.of("Europe/Berlin"));

	@ParameterizedTest // DATAES-716
	@EnumSource(DateFormat.class)
	void shouldCreateConvertersForAllKnownFormats(DateFormat dateFormat) {

		switch (dateFormat) {
			case weekyear:
			case weekyear_week:
			case weekyear_week_day:
				return;
		}

		ElasticsearchDateConverter converter = ElasticsearchDateConverter.of(dateFormat.name());

		assertThat(converter).isNotNull();
	}

	@Test // DATAES-716
	void shouldCreateConvertersForDateFormatPattern() {

		// given
		String pattern = "dd.MM.uuuu";

		// when
		ElasticsearchDateConverter converter = ElasticsearchDateConverter.of(pattern);

		// then
		assertThat(converter).isNotNull();
	}

	@Test // DATAES-716
	void shouldConvertTemporalAccessorToString() {
		LocalDate localDate = LocalDate.of(2019, 12, 27);
		ElasticsearchDateConverter converter = ElasticsearchDateConverter.of(DateFormat.basic_date);

		String formatted = converter.format(localDate);

		assertThat(formatted).isEqualTo("20191227");
	}

	@Test // DATAES-716
	void shouldParseTemporalAccessorFromString() {
		LocalDate localDate = LocalDate.of(2019, 12, 27);
		ElasticsearchDateConverter converter = ElasticsearchDateConverter.of(DateFormat.basic_date);

		LocalDate parsed = converter.parse("20191227", LocalDate.class);

		assertThat(parsed).isEqualTo(localDate);
	}

	@Test // DATAES-792
	void shouldConvertLegacyDateToString() {
		GregorianCalendar calendar = GregorianCalendar
				.from(ZonedDateTime.of(LocalDateTime.of(2020, 4, 19, 19, 44), ZoneId.of("UTC")));
		Date legacyDate = calendar.getTime();
		ElasticsearchDateConverter converter = ElasticsearchDateConverter.of(DateFormat.basic_date_time);

		String formatted = converter.format(legacyDate);

		assertThat(formatted).isEqualTo("20200419T194400.000Z");
	}

	@Test // DATAES-792
	void shouldParseLegacyDateFromString() {
		GregorianCalendar calendar = GregorianCalendar
				.from(ZonedDateTime.of(LocalDateTime.of(2020, 4, 19, 19, 44), ZoneId.of("UTC")));
		Date legacyDate = calendar.getTime();
		ElasticsearchDateConverter converter = ElasticsearchDateConverter.of(DateFormat.basic_date_time);

		Date parsed = converter.parse("20200419T194400.000Z");

		assertThat(parsed).isEqualTo(legacyDate);
	}

	@Test // DATAES-847
	void shouldParseEpochMillisString() {
		Instant instant = Instant.ofEpochMilli(1234568901234L);
		ElasticsearchDateConverter converter = ElasticsearchDateConverter.of(DateFormat.epoch_millis);

		Date parsed = converter.parse("1234568901234");

		assertThat(parsed.toInstant()).isEqualTo(instant);
	}

	@Test // DATAES-847
	void shouldConvertInstantToString() {
		Instant instant = Instant.ofEpochMilli(1234568901234L);
		ElasticsearchDateConverter converter = ElasticsearchDateConverter.of(DateFormat.epoch_millis);

		String formatted = converter.format(instant);

		assertThat(formatted).isEqualTo("1234568901234");
	}

	@Test // DATAES-953
	@DisplayName("should write and read Date with custom format")
	void shouldWriteAndReadDateWithCustomFormat() {

		// only seconds as the format string does not store millis
		long currentTimeSeconds = System.currentTimeMillis() / 1_000;
		Date date = new Date(currentTimeSeconds * 1_000);

		ElasticsearchDateConverter converter = ElasticsearchDateConverter.of("uuuu-MM-dd HH:mm:ss");

		String formatted = converter.format(date);
		Date parsed = converter.parse(formatted);

		assertThat(parsed).isEqualTo(date);
	}

	@Test // DATAES-953
	@DisplayName("should write and read Instant with custom format")
	void shouldWriteAndReadInstantWithCustomFormat() {

		// only seconds as the format string does not store millis
		long currentTimeSeconds = System.currentTimeMillis() / 1_000;
		Instant instant = Instant.ofEpochSecond(currentTimeSeconds);

		ElasticsearchDateConverter converter = ElasticsearchDateConverter.of("uuuu-MM-dd HH:mm:ss");

		String formatted = converter.format(instant);
		Instant parsed = converter.parse(formatted, Instant.class);

		assertThat(parsed).isEqualTo(instant);
	}

	@Test // #1647
	@DisplayName("should convert basic_date")
	void shouldConvertBasicDate() {
		check(ElasticsearchDateConverter.of(DateFormat.basic_date), LocalDate.class);
	}

	@Test // #1647
	@DisplayName("should convert basic_date_time")
	void shouldConvertBasicDateTime() {
		check(ElasticsearchDateConverter.of(DateFormat.basic_date_time), LocalDateTime.class);
	}

	@Test // #1647
	@DisplayName("should convert basic_date_time_no_millis")
	void shouldConvertBasicDateTimeNoMillis() {
		check(ElasticsearchDateConverter.of(DateFormat.basic_date_time_no_millis), LocalDateTime.class);
	}

	@Test // #1647
	@DisplayName("should convert basic_ordinal_date")
	void shouldConvertBasicOrdinalDate() {
		check(ElasticsearchDateConverter.of(DateFormat.basic_ordinal_date), LocalDate.class);
	}

	@Test // #1647
	@DisplayName("should convert basic_ordinal_date_time")
	void shouldConvertBasicOrdinalDateTime() {
		check(ElasticsearchDateConverter.of(DateFormat.basic_ordinal_date_time), LocalDateTime.class);
	}

	@Test // #1647
	@DisplayName("should convert basic_ordinal_date_time_no_millis")
	void shouldConvertBasicOrdinalDateTimeNoMillis() {
		check(ElasticsearchDateConverter.of(DateFormat.basic_ordinal_date_time_no_millis), LocalDateTime.class);
	}

	@Test // #1647
	@DisplayName("should convert basic_time")
	void shouldConvertBasicTime() {
		check(ElasticsearchDateConverter.of(DateFormat.basic_time), LocalTime.class);
	}

	@Test // #1647
	@DisplayName("should convert basic_time_no_millis")
	void shouldConvertBasicTimeNoMillis() {
		check(ElasticsearchDateConverter.of(DateFormat.basic_time_no_millis), LocalTime.class);
	}

	@Test // #1647
	@DisplayName("should convert basic_t_time")
	void shouldConvertBasicTTime() {
		check(ElasticsearchDateConverter.of(DateFormat.basic_t_time), LocalTime.class);
	}

	@Test // #1647
	@DisplayName("should convert basic_t_time_no_millis")
	void shouldConvertBasicTTimeNoMillis() {
		check(ElasticsearchDateConverter.of(DateFormat.basic_t_time_no_millis), LocalTime.class);
	}

	@Test // #1647
	@DisplayName("should convert basic_week_date")
	void shouldConvertBasicWeekDate() {
		check(ElasticsearchDateConverter.of(DateFormat.basic_week_date), LocalDate.class);
	}

	@Test // #1647
	@DisplayName("should convert basic_week_date_time")
	void shouldConvertBasicWeekDateTime() {
		check(ElasticsearchDateConverter.of(DateFormat.basic_week_date_time), LocalDateTime.class);
	}

	@Test // #1647
	@DisplayName("should convert basic_week_date_time_no_millis")
	void shouldConvertBasicWeekDateTimeNoMillis() {
		check(ElasticsearchDateConverter.of(DateFormat.basic_week_date_time_no_millis), LocalDateTime.class);
	}

	@Test // #1647
	@DisplayName("should convert date")
	void shouldConvertDate() {
		check(ElasticsearchDateConverter.of(DateFormat.date), LocalDate.class);
	}

	@Test // #1647
	@DisplayName("should convert date_hour")
	void shouldConvertDateHour() {
		check(ElasticsearchDateConverter.of(DateFormat.date_hour), LocalDateTime.class);
	}

	@Test // #1647
	@DisplayName("should convert date_hour_minute")
	void shouldConvertDateHourMinute() {
		check(ElasticsearchDateConverter.of(DateFormat.date_hour_minute), LocalDateTime.class);
	}

	@Test // #1647
	@DisplayName("should convert date_hour_minute_second")
	void shouldConvertDateHourMinuteSecond() {
		check(ElasticsearchDateConverter.of(DateFormat.date_hour_minute_second), LocalDateTime.class);
	}

	@Test // #1647
	@DisplayName("should convert date_hour_minute_second_fraction")
	void shouldConvertDateHourMinuteSecondFraction() {
		check(ElasticsearchDateConverter.of(DateFormat.date_hour_minute_second_fraction), LocalDateTime.class);
	}

	@Test // #1647
	@DisplayName("should convert date_hour_minute_second_millis")
	void shouldConvertDateHourMinuteSecondMillis() {
		check(ElasticsearchDateConverter.of(DateFormat.date_hour_minute_second_millis), LocalDateTime.class);
	}

	@Test // #1647
	@DisplayName("should convert date_optional_time")
	void shouldConvertDateOptionalTime() {
		check(ElasticsearchDateConverter.of(DateFormat.date_optional_time), LocalDateTime.class);
	}

	@Test // #2676
	@DisplayName("should convert strict_date_optional_time_nanos")
	void shouldConvertStrictDateOptionalTime() {
		check(ElasticsearchDateConverter.of(DateFormat.strict_date_optional_time_nanos), LocalDateTime.class);
	}

	@Test // #1647
	@DisplayName("should convert date_time")
	void shouldConvertDateTime() {
		check(ElasticsearchDateConverter.of(DateFormat.date_time), LocalDateTime.class);
	}

	@Test // #1647
	@DisplayName("should convert date_time_no_millis")
	void shouldConvertDateTimeNoMillis() {
		check(ElasticsearchDateConverter.of(DateFormat.date_time_no_millis), LocalDateTime.class);
	}

	@Test // #1647
	@DisplayName("should convert epoch_millis")
	void shouldConvertEpochMillis() {
		check(ElasticsearchDateConverter.of(DateFormat.epoch_millis), Instant.class);
	}

	@Test // #1647
	@DisplayName("should convert epoch_second")
	void shouldConvertEpochSecond() {
		check(ElasticsearchDateConverter.of(DateFormat.epoch_second), Instant.class);
	}

	@Test // #1647
	@DisplayName("should convert hour")
	void shouldConvertHour() {
		check(ElasticsearchDateConverter.of(DateFormat.hour), LocalTime.class);
	}

	@Test // #1647
	@DisplayName("should convert hour_minute")
	void shouldConvertHourMinute() {
		check(ElasticsearchDateConverter.of(DateFormat.hour_minute), LocalTime.class);
	}

	@Test // #1647
	@DisplayName("should convert hour_minute_second")
	void shouldConvertHourMinuteSecond() {
		check(ElasticsearchDateConverter.of(DateFormat.hour_minute_second), LocalTime.class);
	}

	@Test // #1647
	@DisplayName("should convert hour_minute_second_fraction")
	void shouldConvertHourMinuteSecondFraction() {
		check(ElasticsearchDateConverter.of(DateFormat.hour_minute_second_fraction), LocalTime.class);
	}

	@Test // #1647
	@DisplayName("should convert hour_minute_second_millis")
	void shouldConvertHourMinuteSecondMillis() {
		check(ElasticsearchDateConverter.of(DateFormat.hour_minute_second_millis), LocalTime.class);
	}

	@Test // #1647
	@DisplayName("should convert ordinal_date")
	void shouldConvertOrdinalDate() {
		check(ElasticsearchDateConverter.of(DateFormat.ordinal_date), LocalDate.class);
	}

	@Test // #1647
	@DisplayName("should convert ordinal_date_time")
	void shouldConvertOrdinalDateTime() {
		check(ElasticsearchDateConverter.of(DateFormat.ordinal_date_time), LocalDateTime.class);
	}

	@Test // #1647
	@DisplayName("should convert ordinal_date_time_no_millis")
	void shouldConvertOrdinalDateTimeNoMillis() {
		check(ElasticsearchDateConverter.of(DateFormat.ordinal_date_time_no_millis), LocalDateTime.class);
	}

	@Test // #1647
	@DisplayName("should convert time")
	void shouldConvertTime() {
		check(ElasticsearchDateConverter.of(DateFormat.time), LocalTime.class);
	}

	@Test // #1647
	@DisplayName("should convert time_no_millis")
	void shouldConvertTimeNoMillis() {
		check(ElasticsearchDateConverter.of(DateFormat.time_no_millis), LocalTime.class);
	}

	@Test // #1647
	@DisplayName("should convert t_time")
	void shouldConvertTTime() {
		check(ElasticsearchDateConverter.of(DateFormat.t_time), LocalTime.class);
	}

	@Test // #1647
	@DisplayName("should convert t_time_no_millis")
	void shouldConvertTTimeNoMillis() {
		check(ElasticsearchDateConverter.of(DateFormat.t_time_no_millis), LocalTime.class);
	}

	@Test // #1647
	@DisplayName("should convert week_date")
	void shouldConvertWeekDate() {
		check(ElasticsearchDateConverter.of(DateFormat.week_date), LocalDate.class);
	}

	@Test // #1647
	@DisplayName("should convert week_date_time")
	void shouldConvertWeekDateTime() {
		check(ElasticsearchDateConverter.of(DateFormat.week_date_time), LocalDateTime.class);
	}

	@Test // #1647
	@DisplayName("should convert week_date_time_no_millis")
	void shouldConvertWeekDateTimeNoMillis() {
		check(ElasticsearchDateConverter.of(DateFormat.week_date_time_no_millis), LocalDate.class);
	}

	@Test // #1647
	@DisplayName("should convert year")
	void shouldConvertYear() {
		check(ElasticsearchDateConverter.of(DateFormat.year), Year.class);
	}

	@Test // #1647
	@DisplayName("should convert year_month")
	void shouldConvertYearMonth() {
		check(ElasticsearchDateConverter.of(DateFormat.year_month), YearMonth.class);
	}

	@Test // #1647
	@DisplayName("should convert year_month_day")
	void shouldConvertYearMonthDay() {
		check(ElasticsearchDateConverter.of(DateFormat.year_month_day), LocalDate.class);
	}

	@Test // #1647
	@DisplayName("should convert with combined patterns")
	void shouldConvertWithCombinedPatterns() {
		check(ElasticsearchDateConverter.of("basic_date_time ||invalid-pattern"), LocalDateTime.class);
	}

	private <T extends TemporalAccessor> void check(ElasticsearchDateConverter converter, Class<T> type) {

		String formatted = converter.format(zdt);
		T parsed = converter.parse(formatted, type);

		assertThat(parsed).isNotNull();
	}
}
