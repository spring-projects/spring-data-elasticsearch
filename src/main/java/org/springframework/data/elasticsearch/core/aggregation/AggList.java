package org.springframework.data.elasticsearch.core.aggregation;

import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AggList<T> extends AggAssistant {
    public AggList(String name, AggAssistant... subAggs) {
        super(name, subAggs);
    }

    @Override
    public List<T> collectValue(Aggregation aggregation) {
        List<T> result = new ArrayList<>();

        List buckets = null;
        if (aggregation instanceof StringTerms) {
            buckets = ((StringTerms) aggregation).getBuckets();
        } else if (aggregation instanceof LongTerms) {
            buckets = ((LongTerms) aggregation).getBuckets();
        } else {
            throw new UnsupportedOperationException("Unsupported type (" + aggregation.getClass() + ") for aggList:" + getName());
        }

        Map<String, Object> subItems = new HashMap<>();
        for (Object resultItemObj : buckets) {
            Terms.Bucket resultItem = (Terms.Bucket) resultItemObj;
            String value = resultItem.getKeyAsString();
            subItems.clear();
            subItems.put("docCount", resultItem.getDocCount());
            if (getSubAggs() != null) {
                for (AggAssistant assistant : getSubAggs()) {
                    Object subValue = assistant.collectValue(
                            resultItem.getAggregations()
                                    .asMap()
                                    .get(assistant.getName()));
                    subItems.put(assistant.getName(), subValue);
                }
            }
            T rowValue = createResult(value, subItems);
            if (rowValue != null) {
                result.add(rowValue);
            }
        }
        return result;
    }

    public List<T> readValue(Map<String, Object> subItems) {
        return (List<T>) subItems.get(this.getName());
    }

    public List<T> readValue(Map<String, Object> subItems, List<T> defaultValue) {
        return (List<T>) subItems.getOrDefault(this.getName(), defaultValue);
    }

    public abstract T createResult(String value, Map<String, Object> subItems);

    public static long getDocCount(Map<String, Object> subItems) {
        Long v = (Long) subItems.getOrDefault("docCount", 0l);
        return v.longValue();
    }
}
