package org.springframework.data.elasticsearch.repository.query;


import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.convert.DateTimeConverters;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.util.Assert;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ElasticsearchStringQuery extends AbstractElasticsearchRepositoryQuery{

    private static final Pattern PARAMETER_PLACEHOLDER = Pattern.compile("\\?(\\d+)");
    private String query;

    private final GenericConversionService conversionService = new GenericConversionService();

    {
        if (!conversionService.canConvert(java.util.Date.class, String.class)) {
            conversionService.addConverter(DateTimeConverters.JavaDateConverter.INSTANCE);
        }
        if (!conversionService.canConvert(org.joda.time.ReadableInstant.class, String.class)) {
            conversionService.addConverter(DateTimeConverters.JodaDateTimeConverter.INSTANCE);
        }
        if (!conversionService.canConvert(org.joda.time.LocalDateTime.class, String.class)) {
            conversionService.addConverter(DateTimeConverters.JodaLocalDateTimeConverter.INSTANCE);
        }

    }

    public ElasticsearchStringQuery(ElasticsearchQueryMethod queryMethod, ElasticsearchOperations elasticsearchOperations, String query) {
        super(queryMethod, elasticsearchOperations);
        Assert.notNull(query, "Query cannot be empty");
        this.query = query;
    }

    @Override
    public Object execute(Object[] parameters) {
        ParametersParameterAccessor accessor = new ParametersParameterAccessor(queryMethod.getParameters(), parameters);
        StringQuery stringQuery = createQuery(accessor);
        if(queryMethod.isPageQuery()){
            return  elasticsearchOperations.queryForPage(stringQuery, queryMethod.getEntityInformation().getJavaType());
        }
        return elasticsearchOperations.queryForObject(stringQuery, queryMethod.getEntityInformation().getJavaType());
    }


    protected StringQuery createQuery(ParametersParameterAccessor parameterAccessor) {
        String queryString = replacePlaceholders(this.query, parameterAccessor);
        return new StringQuery(queryString);
    }

    private String replacePlaceholders(String input, ParametersParameterAccessor accessor) {
        Matcher matcher = PARAMETER_PLACEHOLDER.matcher(input);
        String result = input;
        while (matcher.find()) {
            String group = matcher.group();
            int index = Integer.parseInt(matcher.group(1));
            result = result.replace(group, getParameterWithIndex(accessor, index));
        }
        return result;
    }

    private String getParameterWithIndex(ParametersParameterAccessor accessor, int index) {
        Object parameter = accessor.getBindableValue(index);
        if (parameter == null) {
            return "null";
        }
        if (conversionService.canConvert(parameter.getClass(), String.class)) {
            return conversionService.convert(parameter, String.class);
        }
        return parameter.toString();
    }
}
