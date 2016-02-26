package org.springframework.data.elasticsearch.core.partition;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.data.elasticsearch.annotations.PartitionStrategy;
import org.springframework.data.elasticsearch.core.partition.boundaries.DatePartitionBoundary;
import org.springframework.data.elasticsearch.core.partition.boundaries.LongPartitionBoundary;
import org.springframework.data.elasticsearch.core.partition.boundaries.PartitionBoundary;
import org.springframework.data.elasticsearch.core.partition.boundaries.StringPartitionBoundary;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by flefebure on 26/02/2016.
 */
public class PartitionBoundariesTest {

    @Test
    public void testStringBoundaries() {
        StringPartitionBoundary stringBoundary = new StringPartitionBoundary("customer", new String[]{"johndoe", "homersimpson"});
        List<PartitionBoundary> boundaries = new ArrayList<PartitionBoundary>();
        boundaries.add(stringBoundary);
        String[] partitions = new String[]{"customer"};
        PartitionStrategy[] strategies = new PartitionStrategy[]{PartitionStrategy.fixed_string};
        String[] parameters = new String[]{""};
        List<String> slices = PartitionBoundary.getSlices(boundaries,partitions,strategies,parameters);
        Assert.assertEquals(2, slices.size());
        Assert.assertTrue(slices.contains("johndoe"));
        Assert.assertTrue(slices.contains("homersimpson"));
    }

    @Test
    public void testLongBoundaries() {
        LongPartitionBoundary longBoundary = new LongPartitionBoundary("id", 3500000, 5700000);
        List<PartitionBoundary> boundaries = new ArrayList<PartitionBoundary>();
        boundaries.add(longBoundary);
        String[] partitions = new String[]{"id"};
        PartitionStrategy[] strategies = new PartitionStrategy[]{PartitionStrategy.long_range};
        String[] parameters = new String[]{"1000000"};
        List<String> slices = PartitionBoundary.getSlices(boundaries,partitions,strategies,parameters);
        Assert.assertEquals(3, slices.size());
        Assert.assertTrue(slices.contains("3000000"));
        Assert.assertTrue(slices.contains("4000000"));
        Assert.assertTrue(slices.contains("5000000"));

        longBoundary = new LongPartitionBoundary("id", 3500000, 3700000);
        boundaries.clear();
        boundaries.add(longBoundary);
        slices = PartitionBoundary.getSlices(boundaries,partitions,strategies,parameters);
        Assert.assertEquals(1, slices.size());
        Assert.assertTrue(slices.contains("3000000"));
    }

    @Test
    public void testDateBoundaries() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd:HH:mm");
        Date start = sdf.parse("2015/03/02:13:24");
        Date end = sdf.parse("2015/05/15:17:36");
        DatePartitionBoundary dateBoundary = new DatePartitionBoundary("creationDate", start, end);
        List<PartitionBoundary> boundaries = new ArrayList<PartitionBoundary>();
        boundaries.add(dateBoundary);
        String[] partitions = new String[]{"creationDate"};
        PartitionStrategy[] strategies = new PartitionStrategy[]{PartitionStrategy.date_range};
        String[] parameters = new String[]{"YYYYMM"};
        List<String> slices = PartitionBoundary.getSlices(boundaries,partitions,strategies,parameters);
        Assert.assertEquals(3, slices.size());
        Assert.assertTrue(slices.contains("201503"));
        Assert.assertTrue(slices.contains("201504"));
        Assert.assertTrue(slices.contains("201505"));

        start = sdf.parse("2015/03/02:00:00");
        end = sdf.parse("2015/03/02:00:00");
        dateBoundary = new DatePartitionBoundary("creationDate", start, end);
        boundaries.clear();
        boundaries.add(dateBoundary);
        slices = PartitionBoundary.getSlices(boundaries,partitions,strategies,parameters);
        Assert.assertEquals(1, slices.size());
        Assert.assertTrue(slices.contains("201503"));
    }

    @Test
    public void testComplexBoundaries() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd:HH:mm");
        StringPartitionBoundary stringBoundary = new StringPartitionBoundary("customer", new String[]{"johndoe", "homersimpson"});
        List<PartitionBoundary> boundaries = new ArrayList<PartitionBoundary>();
        boundaries.add(stringBoundary);
        Date start = sdf.parse("2015/03/02:13:24");
        Date end = sdf.parse("2015/05/15:17:36");
        DatePartitionBoundary dateBoundary = new DatePartitionBoundary("creationDate", start, end);
        boundaries.add(dateBoundary);
        String[] partitions = new String[]{"customer","creationDate"};
        PartitionStrategy[] strategies = new PartitionStrategy[]{PartitionStrategy.fixed_string, PartitionStrategy.date_range};
        String[] parameters = new String[]{"","YYYYMM"};
        List<String> slices = PartitionBoundary.getSlices(boundaries,partitions,strategies,parameters);
        Assert.assertEquals(6, slices.size());
        Assert.assertTrue(slices.contains("johndoe_201503"));
        Assert.assertTrue(slices.contains("johndoe_201504"));
        Assert.assertTrue(slices.contains("johndoe_201505"));
        Assert.assertTrue(slices.contains("homersimpson_201503"));
        Assert.assertTrue(slices.contains("homersimpson_201504"));
        Assert.assertTrue(slices.contains("homersimpson_201505"));


    }
}
