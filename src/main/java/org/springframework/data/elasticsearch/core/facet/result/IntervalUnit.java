package org.springframework.data.elasticsearch.core.facet.result;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Single term
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Artur Konczak
 * @author Jonathan Yan
 */
public class IntervalUnit {

    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    long key;
    long count;
    long totalCount;
    double total;
    double mean;
    double min;
    double max;

    public IntervalUnit(long key, long count, long totalCount, double total, double mean, double min, double max) {
        this.key = key;
        this.count = count;
        this.totalCount = totalCount;
        this.total = total;
        this.mean = mean;
        this.min = min;
        this.max = max;
    }

    public long getKey() {
        return key;
    }

    public long getCount() {
        return count;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public double getTotal() {
        return total;
    }

    public double getMean() {
        return mean;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    @Override
    public String toString() {
        return "IntervalUnit{" +
                "key=" + format.format(new Date(key)) +
                ", count=" + count +
                ", totalCount=" + totalCount +
                ", total=" + total +
                ", mean=" + mean +
                ", min=" + min +
                ", max=" + max +
                '}';
    }
}
