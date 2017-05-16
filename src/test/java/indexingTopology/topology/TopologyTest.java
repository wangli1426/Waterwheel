package indexingTopology.topology;
import indexingTopology.aggregator.*;
import indexingTopology.bolt.InputStreamReceiver;
import indexingTopology.bolt.InputStreamReceiverServer;
import indexingTopology.bolt.QueryCoordinator;
import indexingTopology.bolt.QueryCoordinatorWithQueryReceiverServer;
import indexingTopology.client.*;
import indexingTopology.data.DataSchema;
import indexingTopology.data.DataTuple;
import indexingTopology.util.DataTupleEquivalentPredicateHint;
import indexingTopology.util.DataTuplePredicate;
import indexingTopology.util.DataTupleSorter;
import indexingTopology.util.TopologyGenerator;
import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.generated.StormTopology;
import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.Assert.*;


/**
 * Created by Robert on 5/15/17.
 */
public class TopologyTest {

    @Test
    public void testSimpleTopologyKeyRangeQuery() throws InterruptedException {
        DataSchema schema = new DataSchema();
        schema.addIntField("a1");
        schema.addDoubleField("a2");
        schema.addLongField("timestamp");
        schema.addVarcharField("a4", 100);
        schema.setPrimaryIndexField("a1");

        final int minIndex = 0;
        final int maxIndex = 100;

        TopologyGenerator<Integer> topologyGenerator = new TopologyGenerator<>();

        InputStreamReceiver inputStreamReceiver = new InputStreamReceiverServer(schema, 10000);
        QueryCoordinator<Integer> coordinator = new QueryCoordinatorWithQueryReceiverServer<>(minIndex, maxIndex, 10001);

        StormTopology topology = topologyGenerator.generateIndexingTopology(schema, minIndex, maxIndex, false, inputStreamReceiver,
                coordinator);

        Config conf = new Config();
        conf.setDebug(false);
        conf.setNumWorkers(1);

        conf.put(Config.WORKER_CHILDOPTS, "-Xmx2048m");
        conf.put(Config.WORKER_HEAP_MEMORY_MB, 2048);


        LocalCluster cluster = new LocalCluster();
        cluster.submitTopology("T0", conf, topology);

        final int tuples = 100000;


        final IngestionClientBatchMode ingestionClient = new IngestionClientBatchMode("localhost", 10000, schema, 1024);
        try {
            ingestionClient.connectWithTimeout(5000);
        } catch (IOException e) {
            e.printStackTrace();
        }
        final QueryClient queryClient = new QueryClient("localhost", 10001);
        try {
            queryClient.connectWithTimeout(5000);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ExecutorService executorService = Executors.newCachedThreadPool();


        boolean fullyExecuted = false;

        for (int i = 0; i < tuples; i++) {
            DataTuple tuple = new DataTuple();
            tuple.add(i % 100);
            tuple.add(3.14);
            tuple.add(100L);
            tuple.add("payload");
            try {
                ingestionClient.appendInBatch(tuple);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            ingestionClient.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // wait for the tuples to be appended.
        Thread.sleep(2000);

        try {

            // full key range query
            QueryResponse response = queryClient.query(new QueryRequest<>(minIndex, maxIndex, Long.MIN_VALUE, Long.MAX_VALUE));
            assertEquals(tuples, response.dataTuples.size());


            //half key range query
            response = queryClient.query(new QueryRequest<>(0, 49, Long.MIN_VALUE, Long.MAX_VALUE));
            assertEquals(tuples/2, response.dataTuples.size());

            //a key range query
            response =  queryClient.query(new QueryRequest<>(0,0, Long.MIN_VALUE, Long.MAX_VALUE));
            assertEquals(tuples/100, response.dataTuples.size());


            fullyExecuted = true;

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            ingestionClient.close();
            queryClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertTrue(fullyExecuted);
        cluster.shutdown();

    }

    @Test
    public void testSimpleTopologyKeyRangeQueryOutOfBoundaries() throws InterruptedException {
        DataSchema schema = new DataSchema();
        schema.addIntField("a1");
        schema.addDoubleField("a2");
        schema.addLongField("timestamp");
        schema.addVarcharField("a4", 100);
        schema.setPrimaryIndexField("a1");

        final int minIndex = 20;
        final int maxIndex = 80;

        TopologyGenerator<Integer> topologyGenerator = new TopologyGenerator<>();

        InputStreamReceiver inputStreamReceiver = new InputStreamReceiverServer(schema, 10000);
        QueryCoordinator<Integer> coordinator = new QueryCoordinatorWithQueryReceiverServer<>(minIndex, maxIndex, 10001);

        StormTopology topology = topologyGenerator.generateIndexingTopology(schema, minIndex, maxIndex, false, inputStreamReceiver,
                coordinator);

        Config conf = new Config();
        conf.setDebug(false);
        conf.setNumWorkers(1);

        conf.put(Config.WORKER_CHILDOPTS, "-Xmx2048m");
        conf.put(Config.WORKER_HEAP_MEMORY_MB, 2048);


        LocalCluster cluster = new LocalCluster();
        cluster.submitTopology("T0", conf, topology);

        final int tuples = 100000;


        final IngestionClientBatchMode ingestionClient = new IngestionClientBatchMode("localhost", 10000, schema, 1024);
        try {
            ingestionClient.connectWithTimeout(5000);
        } catch (IOException e) {
            e.printStackTrace();
        }
        final QueryClient queryClient = new QueryClient("localhost", 10001);
        try {
            queryClient.connectWithTimeout(5000);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ExecutorService executorService = Executors.newCachedThreadPool();


        boolean fullyExecuted = false;

        for (int i = 0; i < tuples; i++) {
            DataTuple tuple = new DataTuple();
            tuple.add(i % 100);
            tuple.add(3.14);
            tuple.add(100L);
            tuple.add("payload");
            try {
                ingestionClient.appendInBatch(tuple);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            ingestionClient.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // wait for the tuples to be appended.
        Thread.sleep(2000);

        try {

            // full key range query
            QueryResponse response = queryClient.query(new QueryRequest<>(0, 100, Long.MIN_VALUE, Long.MAX_VALUE));
            assertEquals(tuples, response.dataTuples.size());


            //half key range query
            response = queryClient.query(new QueryRequest<>(0, 49, Long.MIN_VALUE, Long.MAX_VALUE));
            assertEquals(tuples/2, response.dataTuples.size());

            //a key range query
            response =  queryClient.query(new QueryRequest<>(0,0, Long.MIN_VALUE, Long.MAX_VALUE));
            assertEquals(tuples/100, response.dataTuples.size());


            fullyExecuted = true;

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            ingestionClient.close();
            queryClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertTrue(fullyExecuted);
        cluster.shutdown();

    }

    @Test
    public void testSimpleTopologyPredicateTest() throws InterruptedException {
        DataSchema schema = new DataSchema();
        schema.addIntField("a1");
        schema.addDoubleField("a2");
        schema.addLongField("timestamp");
        schema.addVarcharField("a4", 100);
        schema.setPrimaryIndexField("a1");

        final int minIndex = 0;
        final int maxIndex = 100;

        TopologyGenerator<Integer> topologyGenerator = new TopologyGenerator<>();

        InputStreamReceiver inputStreamReceiver = new InputStreamReceiverServer(schema, 10000);
        QueryCoordinator<Integer> coordinator = new QueryCoordinatorWithQueryReceiverServer<>(minIndex, maxIndex, 10001);

        ArrayList<String> bloomFilterColumns = new ArrayList<>();
        bloomFilterColumns.add("a4");
        StormTopology topology = topologyGenerator.generateIndexingTopology(schema, minIndex, maxIndex, false, inputStreamReceiver,
                coordinator, null, bloomFilterColumns);

        Config conf = new Config();
        conf.setDebug(false);
        conf.setNumWorkers(1);

        conf.put(Config.WORKER_CHILDOPTS, "-Xmx2048m");
        conf.put(Config.WORKER_HEAP_MEMORY_MB, 2048);


        LocalCluster cluster = new LocalCluster();
        cluster.submitTopology("T0", conf, topology);

        final int tuples = 1000000;


        final IngestionClientBatchMode ingestionClient = new IngestionClientBatchMode("localhost", 10000, schema, 1024);
        try {
            ingestionClient.connectWithTimeout(5000);
        } catch (IOException e) {
            e.printStackTrace();
        }
        final QueryClient queryClient = new QueryClient("localhost", 10001);
        try {
            queryClient.connectWithTimeout(5000);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ExecutorService executorService = Executors.newCachedThreadPool();


        boolean fullyExecuted = false;

        for (int i = 0; i < tuples; i++) {
            DataTuple tuple = new DataTuple();
            tuple.add(i % 100);
            tuple.add(3.14);
            tuple.add(100L);
            tuple.add("payload " + i % 100);
            try {
                ingestionClient.appendInBatch(tuple);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            ingestionClient.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // wait for the tuples to be appended.
        Thread.sleep(5000);

        try {

            DataTuplePredicate predicate = t -> schema.getValue("a4", t).equals("payload 0");

//            DataTuplePredicate predicate = new DataTuplePredicate() {
//                @Override
//                public boolean test(DataTuple objects) {
//                    return (int)objects.get(0) == 0;
//                }
//            };

            // without equivalent hint
            {
                // full key range query
                QueryResponse response = queryClient.query(new QueryRequest<>(minIndex, maxIndex, Long.MIN_VALUE, Long.MAX_VALUE, predicate));
                assertEquals(tuples / 100, response.dataTuples.size());


                //half key range query
                response = queryClient.query(new QueryRequest<>(0, 49, Long.MIN_VALUE, Long.MAX_VALUE, predicate));
                assertEquals(tuples / 100, response.dataTuples.size());

                //a key range query
                response = queryClient.query(new QueryRequest<>(0, 0, Long.MIN_VALUE, Long.MAX_VALUE, predicate));
                assertEquals(tuples / 100, response.dataTuples.size());
            }

            // with equivalent hint
            {
                DataTupleEquivalentPredicateHint hint = new DataTupleEquivalentPredicateHint("a4", "payload 0");

                // full key range query
                QueryResponse response = queryClient.query(new QueryRequest<>(minIndex, maxIndex, Long.MIN_VALUE, Long.MAX_VALUE, predicate, null, null, hint));
                assertEquals(tuples / 100, response.dataTuples.size());


                //half key range query
                response = queryClient.query(new QueryRequest<>(0, 49, Long.MIN_VALUE, Long.MAX_VALUE, predicate, null, null, hint));
                assertEquals(tuples / 100, response.dataTuples.size());

                //a key range query
                response = queryClient.query(new QueryRequest<>(0, 0, Long.MIN_VALUE, Long.MAX_VALUE, predicate, null, null, hint));
                assertEquals(tuples / 100, response.dataTuples.size());
            }

            fullyExecuted = true;

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            ingestionClient.close();
            queryClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertTrue(fullyExecuted);
        cluster.shutdown();

    }

    @Test
    public void testSimpleTopologyAggregation() throws InterruptedException {
        DataSchema schema = new DataSchema();
        schema.addIntField("a1");
        schema.addDoubleField("a2");
        schema.addLongField("timestamp");
        schema.addVarcharField("a4", 100);
        schema.setPrimaryIndexField("a1");

        final int minIndex = 0;
        final int maxIndex = 100;

        TopologyGenerator<Integer> topologyGenerator = new TopologyGenerator<>();

        InputStreamReceiver inputStreamReceiver = new InputStreamReceiverServer(schema, 10000);
        QueryCoordinator<Integer> coordinator = new QueryCoordinatorWithQueryReceiverServer<>(minIndex, maxIndex, 10001);

        StormTopology topology = topologyGenerator.generateIndexingTopology(schema, minIndex, maxIndex, false, inputStreamReceiver,
                coordinator);

        Config conf = new Config();
        conf.setDebug(false);
        conf.setNumWorkers(1);

        conf.put(Config.WORKER_CHILDOPTS, "-Xmx2048m");
        conf.put(Config.WORKER_HEAP_MEMORY_MB, 2048);


        LocalCluster cluster = new LocalCluster();
        cluster.submitTopology("T0", conf, topology);

        final int tuples = 100000;


        final IngestionClientBatchMode ingestionClient = new IngestionClientBatchMode("localhost", 10000, schema, 1024);
        try {
            ingestionClient.connectWithTimeout(5000);
        } catch (IOException e) {
            e.printStackTrace();
        }
        final QueryClient queryClient = new QueryClient("localhost", 10001);
        try {
            queryClient.connectWithTimeout(5000);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ExecutorService executorService = Executors.newCachedThreadPool();


        boolean fullyExecuted = false;

        for (int i = 0; i < tuples; i++) {
            DataTuple tuple = new DataTuple();
            tuple.add(i / (tuples / 100));
            tuple.add((double)(i % 1000));
            tuple.add(100L);
            tuple.add("payload");
            try {
                ingestionClient.appendInBatch(tuple);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            ingestionClient.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // wait for the tuples to be appended.
        Thread.sleep(2000);

        try {

            Aggregator<Integer> aggregator = new Aggregator<>(schema, "a1", new AggregateField(new Count(), "*")
            , new AggregateField(new Min<>(), "a2"), new AggregateField(new Max<>(), "a2"));

            // full key range query
            QueryResponse response = queryClient.query(new QueryRequest<>(minIndex, maxIndex, Long.MIN_VALUE,
                    Long.MAX_VALUE, aggregator));
            assertEquals(100, response.dataTuples.size());
            for (DataTuple tuple: response.dataTuples) {
                assertEquals((double)tuples/100, (double)tuple.get(1), 0.0001);
                assertEquals(0.0, (double)tuple.get(2), 0.0001);
                assertEquals(999.0, (double)tuple.get(3), 0.0001);
            }

            //half key range query
            response = queryClient.query(new QueryRequest<>(0, 49, Long.MIN_VALUE, Long.MAX_VALUE, aggregator));
            assertEquals(50, response.dataTuples.size());
            for (DataTuple tuple: response.dataTuples) {
                assertEquals((double)tuples/100, (double)tuple.get(1), 0.0001);
                assertEquals(0.0, (double)tuple.get(2), 0.0001);
                assertEquals(999.0, (double)tuple.get(3), 0.0001);
            }

            //a key range query
            response =  queryClient.query(new QueryRequest<>(0,0, Long.MIN_VALUE, Long.MAX_VALUE, aggregator));
            assertEquals(1, response.dataTuples.size());
            for (DataTuple tuple: response.dataTuples) {
                assertEquals((double)tuples/100, (double)tuple.get(1), 0.0001);
                assertEquals(0.0, (double)tuple.get(2), 0.0001);
                assertEquals(999.0, (double)tuple.get(3), 0.0001);
            }


            fullyExecuted = true;

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            ingestionClient.close();
            queryClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertTrue(fullyExecuted);
        cluster.shutdown();

    }

    @Test
    public void testSimpleTopologySort() throws InterruptedException {
        DataSchema schema = new DataSchema();
        schema.addIntField("a1");
        schema.addDoubleField("a2");
        schema.addLongField("timestamp");
        schema.addVarcharField("a4", 100);
        schema.setPrimaryIndexField("a1");

        final int minIndex = 0;
        final int maxIndex = 100;

        TopologyGenerator<Integer> topologyGenerator = new TopologyGenerator<>();

        InputStreamReceiver inputStreamReceiver = new InputStreamReceiverServer(schema, 10000);
        QueryCoordinator<Integer> coordinator = new QueryCoordinatorWithQueryReceiverServer<>(minIndex, maxIndex, 10001);

        StormTopology topology = topologyGenerator.generateIndexingTopology(schema, minIndex, maxIndex, false, inputStreamReceiver,
                coordinator);

        Config conf = new Config();
        conf.setDebug(false);
        conf.setNumWorkers(1);

        conf.put(Config.WORKER_CHILDOPTS, "-Xmx2048m");
        conf.put(Config.WORKER_HEAP_MEMORY_MB, 2048);


        LocalCluster cluster = new LocalCluster();
        cluster.submitTopology("T0", conf, topology);

        final int tuples = 1000000;


        final IngestionClientBatchMode ingestionClient = new IngestionClientBatchMode("localhost", 10000, schema, 1024);
        try {
            ingestionClient.connectWithTimeout(5000);
        } catch (IOException e) {
            e.printStackTrace();
        }
        final QueryClient queryClient = new QueryClient("localhost", 10001);
        try {
            queryClient.connectWithTimeout(5000);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ExecutorService executorService = Executors.newCachedThreadPool();


        boolean fullyExecuted = false;

        for (int i = 0; i < tuples; i++) {
            DataTuple tuple = new DataTuple();
            tuple.add(i / 1000);
            tuple.add((double)(i % 1000));
            tuple.add(100L);
            tuple.add("payload");
            try {
                ingestionClient.appendInBatch(tuple);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            ingestionClient.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // wait for the tuples to be appended.
        Thread.sleep(2000);

        try {

//            Aggregator<Integer> aggregator = new Aggregator<>(schema, "a1", new AggregateField(new Count(), "*")
//                    , new AggregateField(new Min<>(), "a2"), new AggregateField(new Max<>(), "a2"));

            DataTuplePredicate predicate = t -> (int)schema.getValue("a1", t) < 20;

            DataTupleSorter sorter = (x, y) -> Integer.compare((int)schema.getValue("a1", x),
                    (int)schema.getValue("a1", y));

            // full key range query
            QueryResponse response = queryClient.query(new QueryRequest<>(minIndex, maxIndex, Long.MIN_VALUE,
                    Long.MAX_VALUE, predicate, null, sorter));
            for (int i = 1; i < response.dataTuples.size(); i++) {
                assertTrue((int)response.dataTuples.get(i -1).get(0)<=(int)response.dataTuples.get(i).get(0));
            }

            //half key range query
            response = queryClient.query(new QueryRequest<>(0, 49, Long.MIN_VALUE, Long.MAX_VALUE, predicate,null, sorter));
            for (int i = 1; i < response.dataTuples.size(); i++) {
                assertTrue((int)response.dataTuples.get(i -1).get(0)<=(int)response.dataTuples.get(i).get(0));
            }

            //a key range query
            response =  queryClient.query(new QueryRequest<>(0,0, Long.MIN_VALUE, Long.MAX_VALUE, predicate,null, sorter));
            for (int i = 1; i < response.dataTuples.size(); i++) {
                assertTrue((int)response.dataTuples.get(i -1).get(0)<=(int)response.dataTuples.get(i).get(0));
            }


            fullyExecuted = true;

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            ingestionClient.close();
            queryClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertTrue(fullyExecuted);
        cluster.shutdown();

    }


}
