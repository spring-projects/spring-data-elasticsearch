package org.springframework.data.elasticsearch.core.partition.keys;

import org.springframework.data.elasticsearch.annotations.Partitioner;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by franck.lefebure on 24/02/2016.
 */
public class DatePartition extends org.springframework.data.elasticsearch.core.partition.keys.Partition {

    public DatePartition() {
    }

    @Override
    public String getKeyValue(Object field, String parameter) {
        Date date = (Date)field;
        SimpleDateFormat sdf = new SimpleDateFormat(parameter);
        return sdf.format(field);
    }
    Date start;
    Date end;

    public DatePartition(String partitionKey, Date start, Date end) {
        this.start = start;
        this.end = end;
        setPartitionKey(partitionKey);
    }

    public Date getStart() {
        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public Date getEnd() {
        return end;
    }

    public void setEnd(Date end) {
        this.end = end;
    }

    @Override
    protected List<String> getPartitions(List<String> partitions, Partitioner strategy, String pattern, String separator) {
        List<String> newPartitions = new ArrayList<String>();
        LocalDateTime startTime = LocalDateTime.ofInstant(start.toInstant(), ZoneId.systemDefault());
        LocalDateTime endTime = LocalDateTime.ofInstant(end.toInstant(), ZoneId.systemDefault());
        DateTimeFormatter newSdf = DateTimeFormatter.ofPattern(pattern);
        LocalDateTime slice = null;
        ChronoUnit unit = null;
        if (pattern.equals("YYYYMMddHH")) {
            slice = LocalDateTime.of(startTime.getYear(), startTime.getMonth(), startTime.getDayOfMonth(), startTime.getHour(), 0);
            unit = ChronoUnit.HOURS;
        }
        else if (pattern.equals("YYYYMMdd")) {
            slice = LocalDateTime.of(startTime.getYear(), startTime.getMonth(), startTime.getDayOfMonth(), 0, 0);
            unit = ChronoUnit.DAYS;
        }
        else if (pattern.equals("YYYYMM")) {
            slice = LocalDateTime.of(startTime.getYear(), startTime.getMonth(), 1, 0, 0);
            unit = ChronoUnit.MONTHS;
        }
        else {
            slice = LocalDateTime.of(startTime.getYear(), 1, 1, 0, 0);
            unit = ChronoUnit.MONTHS;
        }
        newPartitions.add(newSdf.format(slice));
        slice = slice.plus(1, unit);

        while (slice.isBefore(endTime)) {
            newPartitions.add(newSdf.format(slice));
            slice = slice.plus(1, unit);
        }
        return appendPartitions(partitions, newPartitions, separator);
    }
}
