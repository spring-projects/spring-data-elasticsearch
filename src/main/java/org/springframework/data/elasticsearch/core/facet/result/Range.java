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


    public Range(Double from, Double to, long count, double total) {
        this.from = from;
        this.to = to;
        this.count = count;
        this.total = total;
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
}
