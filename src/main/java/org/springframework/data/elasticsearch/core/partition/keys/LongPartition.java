package org.springframework.data.elasticsearch.core.partition.keys;

import org.springframework.data.elasticsearch.annotations.Partitioner;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by franck.lefebure on 24/02/2016.
 */
public class LongPartition extends org.springframework.data.elasticsearch.core.partition.keys.Partition {

    public LongPartition() {
    }

    @Override
    public String getKeyValue(Object field, String parameter) {
        String s = field.toString();
        long key = Long.parseLong(s);
        long slice = Long.parseLong(parameter);
        return new Long(key/slice*slice).toString();
    }

    long start;
    long end;

    public LongPartition(String partitionKey, long start, long end) {
        this.start = start;
        this.end = end;
        setPartitionKey(partitionKey);
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    @Override
    protected List<String> getPartitions(List<String> partitions, Partitioner strategy, String parameter, String separator) {
        List<String> newPartitions = new ArrayList<String>();
        long slice = Long.parseLong(parameter);
        long aSlice = start/slice*slice;
        newPartitions.add(new Long(aSlice).toString());
        aSlice = aSlice+slice;
        while (aSlice <= end) {
            newPartitions.add(new Long(aSlice).toString());
            aSlice = aSlice+slice;
        }
        return appendPartitions(partitions, newPartitions, separator);
    }
}
