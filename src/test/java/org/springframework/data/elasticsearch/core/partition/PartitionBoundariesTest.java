package org.springframework.data.elasticsearch.core.partition;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.data.elasticsearch.annotations.Partitioner;
import org.springframework.data.elasticsearch.core.partition.keys.DatePartition;
import org.springframework.data.elasticsearch.core.partition.keys.LongPartition;
import org.springframework.data.elasticsearch.core.partition.keys.Partition;
import org.springframework.data.elasticsearch.core.partition.keys.StringPartition;

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
        StringPartition stringBoundary = new StringPartition("customer", new String[]{"johndoe", "homersimpson"});
        List<Partition> boundaries = new ArrayList<Partition>();
        boundaries.add(stringBoundary);
        String[] partitions = new String[]{"customer"};
        Partitioner[] strategies = new Partitioner[]{Partitioner.fixed_string};
        String[] parameters = new String[]{""};
        List<String> slices = Partition.getPartitions(boundaries,partitions,strategies,parameters,"_");
        Assert.assertEquals(2, slices.size());
        Assert.assertTrue(slices.contains("johndoe"));
        Assert.assertTrue(slices.contains("homersimpson"));
    }

    @Test
    public void testLongBoundaries() {
        LongPartition longBoundary = new LongPartition("id", 3500000, 5700000);
        List<Partition> boundaries = new ArrayList<Partition>();
        boundaries.add(longBoundary);
        String[] partitions = new String[]{"id"};
        Partitioner[] strategies = new Partitioner[]{Partitioner.long_range};
        String[] parameters = new String[]{"1000000"};
        List<String> slices = Partition.getPartitions(boundaries,partitions,strategies,parameters,"_");
        Assert.assertEquals(3, slices.size());
        Assert.assertTrue(slices.contains("3000000"));
        Assert.assertTrue(slices.contains("4000000"));
        Assert.assertTrue(slices.contains("5000000"));

        longBoundary = new LongPartition("id", 3500000, 3700000);
        boundaries.clear();
        boundaries.add(longBoundary);
        slices = Partition.getPartitions(boundaries,partitions,strategies,parameters,"_");
        Assert.assertEquals(1, slices.size());
        Assert.assertTrue(slices.contains("3000000"));
    }

    @Test
    public void testDateBoundaries() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd:HH:mm");
        Date start = sdf.parse("2015/03/02:13:24");
        Date end = sdf.parse("2015/05/15:17:36");
        DatePartition dateBoundary = new DatePartition("creationDate", start, end);
        List<Partition> boundaries = new ArrayList<Partition>();
        boundaries.add(dateBoundary);
        String[] partitions = new String[]{"creationDate"};
        Partitioner[] strategies = new Partitioner[]{Partitioner.date_range};
        String[] parameters = new String[]{"YYYYMM"};
        List<String> slices = Partition.getPartitions(boundaries,partitions,strategies,parameters,"_");
        Assert.assertEquals(3, slices.size());
        Assert.assertTrue(slices.contains("201503"));
        Assert.assertTrue(slices.contains("201504"));
        Assert.assertTrue(slices.contains("201505"));

        start = sdf.parse("2015/03/02:00:00");
        end = sdf.parse("2015/03/02:00:00");
        dateBoundary = new DatePartition("creationDate", start, end);
        boundaries.clear();
        boundaries.add(dateBoundary);
        slices = Partition.getPartitions(boundaries,partitions,strategies,parameters,"_");
        Assert.assertEquals(1, slices.size());
        Assert.assertTrue(slices.contains("201503"));
    }

    @Test
    public void testDailyDateBoundaries() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd:HH:mm");
        Date start = sdf.parse("2015/08/05:13:24");
        Date end = sdf.parse("2015/08/07:17:36");
        DatePartition dateBoundary = new DatePartition("creationDate", start, end);
        List<Partition> boundaries = new ArrayList<Partition>();
        boundaries.add(dateBoundary);
        String[] partitions = new String[]{"creationDate"};
        Partitioner[] strategies = new Partitioner[]{Partitioner.date_range};
        String[] parameters = new String[]{"YYYYMMDD"};
        List<String> slices = Partition.getPartitions(boundaries,partitions,strategies,parameters,"_");
        Assert.assertEquals(3, slices.size());
        Assert.assertTrue(slices.contains("20150805"));
        Assert.assertTrue(slices.contains("20150806"));
        Assert.assertTrue(slices.contains("20150807"));
    }


    @Test
    public void testComplexBoundaries() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd:HH:mm");
        StringPartition stringBoundary = new StringPartition("customer", new String[]{"johndoe", "homersimpson"});
        List<Partition> boundaries = new ArrayList<Partition>();
        boundaries.add(stringBoundary);
        Date start = sdf.parse("2015/03/02:13:24");
        Date end = sdf.parse("2015/05/15:17:36");
        DatePartition dateBoundary = new DatePartition("creationDate", start, end);
        boundaries.add(dateBoundary);
        String[] partitions = new String[]{"customer","creationDate"};
        Partitioner[] strategies = new Partitioner[]{Partitioner.fixed_string, Partitioner.date_range};
        String[] parameters = new String[]{"","YYYYMM"};
        List<String> slices = Partition.getPartitions(boundaries,partitions,strategies,parameters,"_");
        Assert.assertEquals(6, slices.size());
        Assert.assertTrue(slices.contains("johndoe_201503"));
        Assert.assertTrue(slices.contains("johndoe_201504"));
        Assert.assertTrue(slices.contains("johndoe_201505"));
        Assert.assertTrue(slices.contains("homersimpson_201503"));
        Assert.assertTrue(slices.contains("homersimpson_201504"));
        Assert.assertTrue(slices.contains("homersimpson_201505"));


    }
}
