package org.springframework.data.elasticsearch.core.facet.result;

/**
 * Single range
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Artur Konczak
 * @author Jonathan Yan
 */
public class Range {

    private Double from;
    private Double to;
    private long count;
    private double total;
    private double totalCount;
    private double min = Double.POSITIVE_INFINITY;
    private double max = Double.NEGATIVE_INFINITY;

    public Range(Double from, Double to, long count, double total, double totalCount, double min, double max) {
        this.from = from;
        this.to = to;
        this.count = count;
        this.total = total;
        this.totalCount = totalCount;
        this.min = min;
        this.max = max;
    }

    public Double getFrom() {
        return from;
    }

    public Double getTo() {
        return to;
    }

    /**
     * Return number of documents in range
     *
     * @return
     */
    public long getCount() {
        return count;
    }

    public double getTotal() {
        return total;
    }

    public double getTotalCount() {
        return totalCount;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }
}
