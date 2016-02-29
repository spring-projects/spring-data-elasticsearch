package org.springframework.data.elasticsearch.core.partition.keys;

import org.springframework.data.elasticsearch.annotations.Partitioner;

import java.util.Arrays;
import java.util.List;

/**
 * Created by franck.lefebure on 24/02/2016.
 */
public class StringPartition extends org.springframework.data.elasticsearch.core.partition.keys.Partition {

    public StringPartition() {
    }

    @Override
    public String getKeyValue(Object field, String parameter) {
        return field.toString().toLowerCase().trim();
    }

    String[] partitionNames;

    public StringPartition(String partitionKey, String[] partitionNames) {
        this.partitionNames = partitionNames;
        setPartitionKey(partitionKey);
    }

    public String[] getPartitionNames() {
        return partitionNames;
    }

    public void setPartitionNames(String[] partitionNames) {
        this.partitionNames = partitionNames;
    }

    @Override
    protected List<String> getPartitions(List<String> partitions, Partitioner strategy, String param, String separator) {
        List<String> newPartitions = Arrays.asList(this.partitionNames);
        return appendPartitions(partitions, newPartitions, separator);
    }
}
