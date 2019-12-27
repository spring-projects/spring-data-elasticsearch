package org.springframework.data.elasticsearch.core.convert;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.data.elasticsearch.annotations.DateFormat;

/**
 * @author Peter-Josef Meisch
 */
class ElasticsearchDateConverterTests {

	@ParameterizedTest
	@EnumSource(DateFormat.class)
	void shouldCreateConvertersForAllKnownFormats(DateFormat dateFormat) {

		if (dateFormat == DateFormat.none) {
			return;
		}
		String pattern = (dateFormat != DateFormat.custom) ? dateFormat.name() : "dd.MM.yyyy";

		ElasticsearchDateConverter converter = ElasticsearchDateConverter.of(pattern);

		assertThat(converter).isNotNull();
	}

	@Test
	void shouldConvertToString() {
		LocalDate localDate = LocalDate.of(2019, 12, 27);
		ElasticsearchDateConverter converter = ElasticsearchDateConverter.of(DateFormat.basic_date);

		String formatted = converter.format(localDate);

		assertThat(formatted).isEqualTo("20191227");
	}

	@Test
	void shouldParseFromString() {
		LocalDate localDate = LocalDate.of(2019, 12, 27);
		ElasticsearchDateConverter converter = ElasticsearchDateConverter.of(DateFormat.basic_date);

		LocalDate parsed = converter.parse("20191227", LocalDate.class);

		assertThat(parsed).isEqualTo(localDate);
	}
}
