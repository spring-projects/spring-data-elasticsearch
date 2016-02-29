package org.springframework.data.elasticsearch.core.partition.keys;

import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.annotations.Partitioner;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by franck.lefebure on 24/02/2016.
 */
public abstract class Partition {

    String partitionKey;

    public String getPartitionKey() {
        return partitionKey;
    }

    public void setPartitionKey(String partitionKey) {
        this.partitionKey = partitionKey;
    }

    public abstract String getKeyValue(Object field, String parameter);

    protected abstract List<String> getPartitions(List<String> slices, Partitioner strategy, String param, String separator);

    public static List<String> getPartitions(List<Partition> partitions, String[] partitionKeys, Partitioner[] strategies, String[] partitionParameters, String separator) {
        List<String> slices = new ArrayList();
        for (int i = 0; i < partitionKeys.length ; i++) {
            Partition boundary = getPartitionForField(partitions, partitionKeys[i]);
            if (boundary == null)
                throw new ElasticsearchException("missing partition for partition key "+partitionKeys[i]);
            slices = boundary.getPartitions(slices, strategies[i], partitionParameters[i], separator);

        }
        return slices;
    }

    private static Partition getPartitionForField(List<Partition> partitions, String field) {
        for (Partition partition : partitions) {
            if (partition.getPartitionKey().equals(field)) {
                return partition;
            }
        }
        return null;
    }
    protected List<String> appendPartitions(List<String> partitions, List<String> newPartitions, String separator) {
        if (partitions.size() == 0) {
            return newPartitions;
        }
        else {
            List<String> newerPartitions = new ArrayList<String>();
            for (String slice : partitions) {
                for (String newSlice : newPartitions) {
                    newerPartitions.add(slice+separator+newSlice);
                }
            }
            return newerPartitions;
        }
    }

}

