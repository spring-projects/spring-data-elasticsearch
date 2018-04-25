package org.springframework.data.elasticsearch.core.aggregation;

import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.support.ValuesSourceAggregationBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class AggAssistant {
    private String name;
    private AggAssistant[] subAggs;
    public AggAssistant(String name, AggAssistant... subAggs) {
        this.name = name;
        this.subAggs = subAggs;
    }
    protected abstract AbstractAggregationBuilder createBuilder();
    public abstract Object collectValue(Aggregation aggregation);

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AggAssistant[] getSubAggs() {
        return subAggs;
    }

    public void setSubAggs(AggAssistant[] subAggs) {
        this.subAggs = subAggs;
    }

    public AbstractAggregationBuilder toAggBuilder() {
        AbstractAggregationBuilder builder = createBuilder();
        if (subAggs != null) {
            for (AggAssistant assistant : subAggs) {
                builder.subAggregation(assistant.toAggBuilder());
            }
        }
        return builder;
    }

    public static AggValue value(String name, String fieldCode, Function<String, ? extends ValuesSourceAggregationBuilder> creator) {
        return new AggValue(name) {
            @Override
            protected AbstractAggregationBuilder createBuilder() {
                return creator.apply(name).field(fieldCode);
            }
        };
    }

    public static <T> AggList<T> listField(String name, String fieldCode,
                                           BiFunction<String, Map<String, Object>, T> createResult,
                                           AggAssistant... subAggs) {
        return new AggList<T>(name, subAggs) {
            @Override
            protected AbstractAggregationBuilder createBuilder() {
                return AggregationBuilders.terms(getName())
                        .field(fieldCode)
                        .size(1000);
            }

            @Override
            public T createResult(String value, Map<String, Object> subItems) {
                return createResult.apply(value, subItems);
            }
        };
    }

    public static <T> AggList<T> listScript(String name, Script script,
                                            BiFunction<String, Map<String, Object>, T> createResult,
                                            AggAssistant... subAggs) {
        return new AggList<T>(name, subAggs) {
            @Override
            protected AbstractAggregationBuilder createBuilder() {
                return AggregationBuilders.terms(getName())
                        .script(script)
                        .size(1000);
            }

            @Override
            public T createResult(String value, Map<String, Object> subItems) {
                return createResult.apply(value, subItems);
            }
        };
    }

    public static <T> AggList<T> listScriptId(String name, String scriptId,
                                              BiFunction<String, Map<String, Object>, T> createResult,
                                              AggAssistant... subAggs) {
        return new AggList<T>(name, subAggs) {
            @Override
            protected AbstractAggregationBuilder createBuilder() {
                return AggregationBuilders.terms(getName())
                        .script(new Script(ScriptType.STORED, Script.DEFAULT_SCRIPT_LANG, scriptId, new HashMap<>()))
                        .size(1000);
            }

            @Override
            public T createResult(String value, Map<String, Object> subItems) {
                return createResult.apply(value, subItems);
            }
        };
    }

    public static <T> AggList<T> listScriptCode(String name, String scriptCode,
                                                BiFunction<String, Map<String, Object>, T> createResult,
                                                AggAssistant... subAggs) {
        return new AggList<T>(name, subAggs) {
            @Override
            protected AbstractAggregationBuilder createBuilder() {
                return AggregationBuilders.terms(getName())
                        .script(new Script(scriptCode))
                        .size(1000);
            }

            @Override
            public T createResult(String value, Map<String, Object> subItems) {
                return createResult.apply(value, subItems);
            }
        };
    }
}
