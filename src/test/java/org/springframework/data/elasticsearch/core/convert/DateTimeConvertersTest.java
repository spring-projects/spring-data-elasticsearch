package org.springframework.data.elasticsearch.core.convert;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Calendar;
import java.util.TimeZone;


public class DateTimeConvertersTest {
    @Test
    public void testJodaDateTimeConverterWithNullValue() {
        Assert.assertNull(DateTimeConverters.JodaDateTimeConverter.INSTANCE.convert(null));
    }

    @Test
    public void testJodaDateTimeConverter() {
        DateTime dateTime = new DateTime(2013, 1,24 , 6, 35, 0, DateTimeZone.UTC);
        Assert.assertEquals("2013-01-24T06:35:00.000Z",
                DateTimeConverters.JodaDateTimeConverter.INSTANCE.convert(dateTime));
    }

    @Test
    public void testJodaLocalDateTimeConverterWithNullValue() {
        Assert.assertNull(DateTimeConverters.JodaLocalDateTimeConverter.INSTANCE.convert(null));
    }

    @Test
    public void testJodaLocalDateTimeConverter() {
        LocalDateTime dateTime = new LocalDateTime(new DateTime(2013, 1,24, 6, 35, 0, DateTimeZone.UTC).getMillis(),
                DateTimeZone.UTC);
        Assert.assertEquals("2013-01-24T06:35:00.000Z",
                DateTimeConverters.JodaLocalDateTimeConverter.INSTANCE.convert(dateTime));
    }

    @Test
    public void testJavaDateConverterWithNullValue() {
        Assert.assertNull(DateTimeConverters.JavaDateConverter.INSTANCE.convert(null));
    }

    @Test
    public void testJavaDateConverter() {
        DateTime dateTime = new DateTime(2013, 1,24, 6, 35, 0, DateTimeZone.UTC);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(dateTime.getMillis());

        Assert.assertEquals("2013-01-24T06:35:00.000Z",
                DateTimeConverters.JavaDateConverter.INSTANCE.convert(calendar.getTime()));
    }
}
