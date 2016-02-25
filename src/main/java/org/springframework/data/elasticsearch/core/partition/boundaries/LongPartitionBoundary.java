package org.springframework.data.elasticsearch.core.partition.boundaries;

import org.springframework.data.elasticsearch.annotations.PartitionStrategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by flefebure on 25/02/2016.
 */
public class LongPartitionBoundary extends PartitionBoundary {
    long start;
    long end;

    public LongPartitionBoundary(String partitionKey, long start, long end) {
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

    protected List<String> getSlices(List<String> slices, PartitionStrategy strategy) {
        List<String> newSlices = new ArrayList<String>();
        long slice = (Long)strategy.getSlicer();
        long aSlice = start/slice*slice;
        newSlices.add(new Long(aSlice).toString());
        while (aSlice <= end) {
            aSlice = aSlice+slice;
            newSlices.add(new Long(aSlice).toString());
        }
        return appendSlices(slices, newSlices);
    }
}
