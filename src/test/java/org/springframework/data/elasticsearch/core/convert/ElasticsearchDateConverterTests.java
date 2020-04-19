package org.springframework.data.elasticsearch.core.convert;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.data.elasticsearch.annotations.DateFormat;

/**
 * @author Peter-Josef Meisch
 */
class ElasticsearchDateConverterTests {

	@ParameterizedTest // DATAES-716
	@EnumSource(DateFormat.class)
	void shouldCreateConvertersForAllKnownFormats(DateFormat dateFormat) {

		if (dateFormat == DateFormat.none) {
			return;
		}
		String pattern = (dateFormat != DateFormat.custom) ? dateFormat.name() : "dd.MM.uuuu";

		ElasticsearchDateConverter converter = ElasticsearchDateConverter.of(pattern);

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
				.from(ZonedDateTime.of(LocalDateTime.of(2020, 04, 19, 19, 44), ZoneId.of("UTC")));
		Date legacyDate = calendar.getTime();
		ElasticsearchDateConverter converter = ElasticsearchDateConverter.of(DateFormat.basic_date_time);

		String formatted = converter.format(legacyDate);

		assertThat(formatted).isEqualTo("20200419T194400.000Z");
	}

	@Test // DATAES-792
	void shouldParseLegacyDateFromString() {
		GregorianCalendar calendar = GregorianCalendar
				.from(ZonedDateTime.of(LocalDateTime.of(2020, 04, 19, 19, 44), ZoneId.of("UTC")));
		Date legacyDate = calendar.getTime();
		ElasticsearchDateConverter converter = ElasticsearchDateConverter.of(DateFormat.basic_date_time);

		Date parsed = converter.parse("20200419T194400.000Z");

		assertThat(parsed).isEqualTo(legacyDate);
	}
}
