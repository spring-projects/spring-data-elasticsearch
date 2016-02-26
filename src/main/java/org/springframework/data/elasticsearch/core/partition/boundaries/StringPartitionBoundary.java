package org.springframework.data.elasticsearch.core.partition.boundaries;

import org.springframework.data.elasticsearch.annotations.PartitionStrategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by flefebure on 25/02/2016.
 */
public class StringPartitionBoundary extends PartitionBoundary {

    String[] partitions;

    public StringPartitionBoundary(String partitionKey, String[] partitions) {
        this.partitions = partitions;
        setPartitionKey(partitionKey);
    }

    public String[] getPartitions() {
        return partitions;
    }

    public void setPartitions(String[] partitions) {
        this.partitions = partitions;
    }

    protected List<String> getSlices(List<String> slices, PartitionStrategy strategy, String param) {
        List<String> newSlices = Arrays.asList(partitions);
        return appendSlices(slices, newSlices);
    }
}
