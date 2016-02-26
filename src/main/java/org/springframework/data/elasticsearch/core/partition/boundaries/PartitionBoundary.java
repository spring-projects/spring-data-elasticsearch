package org.springframework.data.elasticsearch.core.partition.boundaries;

import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.annotations.PartitionStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by flefebure on 25/02/2016.
 */
public abstract class PartitionBoundary {

    String partitionKey;

    public String getPartitionKey() {
        return partitionKey;
    }

    public void setPartitionKey(String partitionKey) {
        this.partitionKey = partitionKey;
    }

    public static List<String> getSlices(List<PartitionBoundary> boundaries, String[] partitions, PartitionStrategy[] strategies, String[] params) {
        List<String> slices = new ArrayList();
        for (int i = 0; i < partitions.length ; i++) {
            PartitionBoundary boundary = getBoundaryForField(boundaries, partitions[i]);
            if (boundary == null)
                throw new ElasticsearchException("missing partition boundary for partition key "+partitions[i]);
            slices = boundary.getSlices(slices, strategies[i], params[i]);

        }
        return slices;
    }

    private static PartitionBoundary getBoundaryForField(List<PartitionBoundary> boundaries, String field) {
        for (PartitionBoundary boundary : boundaries) {
            if (boundary.getPartitionKey().equals(field)) {
                return boundary;
            }
        }
        return null;
    }

    protected List<String> appendSlices(List<String> slices, List<String> newSlices) {
        if (slices.size() == 0) {
            return newSlices;
        }
        else {
            List<String> newerSlices = new ArrayList<String>();
            for (String slice : slices) {
                for (String newSlice : newSlices) {
                    newerSlices.add(slice+"_"+newSlice);
                }
            }
            return newerSlices;
        }
    }

    protected abstract List<String> getSlices(List<String> slices, PartitionStrategy strategy, String param);
}
