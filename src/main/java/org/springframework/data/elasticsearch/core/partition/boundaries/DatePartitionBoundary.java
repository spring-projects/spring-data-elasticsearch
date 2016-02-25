package org.springframework.data.elasticsearch.core.partition.boundaries;

import org.springframework.data.elasticsearch.annotations.PartitionStrategy;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by flefebure on 25/02/2016.
 */
public class DatePartitionBoundary  extends PartitionBoundary {
    Date start;
    Date end;

    public DatePartitionBoundary(String partitionKey, Date start, Date end) {
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

    protected List<String> getSlices(List<String> slices, PartitionStrategy strategy) {
        List<String> newSlices = new ArrayList<String>();
        String sdf = (String)strategy.getSlicer();
        LocalDateTime startTime = LocalDateTime.ofInstant(start.toInstant(), ZoneId.systemDefault());
        LocalDateTime endTime = LocalDateTime.ofInstant(end.toInstant(), ZoneId.systemDefault());
        DateTimeFormatter newSdf = DateTimeFormatter.ofPattern(sdf);
        LocalDateTime slice = null;
        ChronoUnit unit = null;
        if (sdf.equals("YYYYMMDDHH")) {
            slice = LocalDateTime.of(startTime.getYear(), startTime.getMonth(), startTime.getDayOfMonth(), startTime.getHour(), 0);
            unit = ChronoUnit.HOURS;
        }
        else if (sdf.equals("YYYYMMDD")) {
            slice = LocalDateTime.of(startTime.getYear(), startTime.getMonth(), startTime.getDayOfMonth(), 0, 0);
            unit = ChronoUnit.DAYS;
        }
        else if (sdf.equals("YYYYMM")) {
            slice = LocalDateTime.of(startTime.getYear(), startTime.getMonth(), 1, 0, 0);
            unit = ChronoUnit.MONTHS;
        }
        else {
            slice = LocalDateTime.of(startTime.getYear(), 1, 1, 0, 0);
            unit = ChronoUnit.MONTHS;
        }
        newSlices.add(newSdf.format(slice));

        while (startTime.isBefore(endTime)) {
            slice = slice.plus(1, unit);
            newSlices.add(newSdf.format(slice));
        }
        return appendSlices(slices, newSlices);
    }
}
