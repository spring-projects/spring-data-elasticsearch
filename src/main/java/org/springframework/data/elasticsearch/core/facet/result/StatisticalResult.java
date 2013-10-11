package org.springframework.data.elasticsearch.core.facet.result;

import org.springframework.data.elasticsearch.core.facet.AbstactFacetResult;
import org.springframework.data.elasticsearch.core.facet.FacetType;

/**
 * @author Petar Tahchiev
 */
public class StatisticalResult extends AbstactFacetResult {

    private long count;

    private double max;

    private double min;

    private double mean;

    private double stdDeviation;

    private double sumOfSquares;

    private double total;

    private double variance;

    public StatisticalResult(String name, long count, double max, double min, double mean, double stdDeviation, double sumOfSquares, double total, double variance) {
        super(name, FacetType.statistical);
        this.count = count;
        this.max = max;
        this.min = min;
        this.mean = mean;
        this.stdDeviation = stdDeviation;
        this.sumOfSquares = sumOfSquares;
        this.total = total;
        this.variance = variance;
    }

    public long getCount() {
        return count;
    }

    public double getMax() {
        return max;
    }

    public double getMin() {
        return min;
    }

    public double getMean() {
        return mean;
    }

    public double getStdDeviation() {
        return stdDeviation;
    }

    public double getSumOfSquares() {
        return sumOfSquares;
    }

    public double getTotal() {
        return total;
    }

    public double getVariance() {
        return variance;
    }
}
