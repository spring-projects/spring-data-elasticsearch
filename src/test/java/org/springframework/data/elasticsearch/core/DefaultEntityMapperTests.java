package org.springframework.data.elasticsearch.core;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.elasticsearch.Car;
import org.springframework.data.elasticsearch.CarBuilder;

import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Artur Konczak
 */
public class DefaultEntityMapperTests {

    public static final String JSON_STRING = "{\"name\":\"Grat\",\"model\":\"Ford\"}";
    public static final String CAR_MODEL = "Ford";
    public static final String CAR_NAME = "Grat";
    DefaultEntityMapper entityMapper;

    @Before
    public void init(){
        entityMapper = new DefaultEntityMapper();
    }

    @Test
    public void shouldMapObjectToJsonString() throws IOException {
        //Given

        //When
        String jsonResult = entityMapper.mapToString(new CarBuilder().model(CAR_MODEL).name(CAR_NAME).build());

        //Then
        assertThat(jsonResult, is(JSON_STRING));
    }

    @Test
    public void shouldMapJsonStringToObject() throws IOException {
        //Given

        //When
        Car result = entityMapper.mapToObject(JSON_STRING,Car.class);

        //Then
        assertThat(result.getName(),is(CAR_NAME));
        assertThat(result.getModel(),is(CAR_MODEL));
    }

}
