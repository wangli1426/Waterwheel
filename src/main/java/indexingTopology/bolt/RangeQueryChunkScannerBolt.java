package indexingTopology.bolt;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import indexingTopology.Config.Config;
import indexingTopology.NormalDistributionIndexingAndRangeQueryTopology;
import indexingTopology.NormalDistributionIndexingTopology;
import indexingTopology.util.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by acelzj on 11/15/16.
 */
public class RangeQueryChunkScannerBolt extends BaseRichBolt{

    OutputCollector collector;

    private int bTreeOder;

    public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
        collector = outputCollector;
        bTreeOder = 4;
    }

    public void execute(Tuple tuple) {
        Long queryId = tuple.getLong(0);
        Double leftKey = tuple.getDouble(1);
        Double rightKey = tuple.getDouble(2);
//        ArrayList<String> fileNames = (ArrayList) tuple.getValue(2);
        String fileName = (String) tuple.getValue(3);
        RandomAccessFile file = null;
        ArrayList<byte[]> serializedTuples = new ArrayList<byte[]>();
//        for (String fileName : fileNames) {
            try {
                FileSystemHandler fileSystemHandler = new LocalFileSystemHandler("/home/acelzj");
                fileSystemHandler.openFile("/", fileName);
                byte[] serializedTree = new byte[Config.TEMPLATE_SIZE];
                DeserializationHelper deserializationHelper = new DeserializationHelper();
                BytesCounter counter = new BytesCounter();

                fileSystemHandler.readBytesFromFile(serializedTree);
                BTree deserializedTree = deserializationHelper.deserializeBTree(serializedTree, bTreeOder, counter);
                int offset = deserializedTree.getOffsetOfLeaveNodeShouldContainKey(leftKey);
                long length = fileSystemHandler.getLengthOfFile("/", fileName);
                while (offset < length) {
                    byte[] lengthInByte = new byte[4];
                    fileSystemHandler.seek(offset);
                    fileSystemHandler.readBytesFromFile(lengthInByte);
                    int lengthOfLeaveInBytes = ByteBuffer.wrap(lengthInByte, 0, 4).getInt();
                    if (lengthOfLeaveInBytes == 0) {
                        break;
                    }
                    byte[] leafInByte = new byte[lengthOfLeaveInBytes];
                    fileSystemHandler.seek(offset + 4);
                    fileSystemHandler.readBytesFromFile(leafInByte);
                    BTreeLeafNode deserializedLeaf = deserializationHelper.deserializeLeaf(leafInByte,
                            bTreeOder, counter);
                    ArrayList<byte[]> tuples = deserializedLeaf.rangeSearchAndGetTuples(leftKey, rightKey);
                    if (tuples.size() == 0) {
                        break;
                    } else {
                        serializedTuples.addAll(tuples);
                    }
                    offset = offset + lengthOfLeaveInBytes + 4;
                }
                fileSystemHandler.closeFile();









                /*
                file = new RandomAccessFile("/home/acelzj/" + fileName, "r");
                byte[] serializedTree = new byte[Config.TEMPLATE_SIZE];
                DeserializationHelper deserializationHelper = new DeserializationHelper();
                BytesCounter counter = new BytesCounter();

                file.read(serializedTree, 0, Config.TEMPLATE_SIZE);
                BTree deserializedTree = deserializationHelper.deserializeBTree(serializedTree, bTreeOder, counter);
                int offset = deserializedTree.getOffsetOfLeaveNodeShouldContainKey(leftKey);
                while (offset < file.length()) {
//                    System.out.println("Offset " + offset);
                    byte[] lengthInByte = new byte[4];
                    file.seek(offset);
                    file.read(lengthInByte);
                    int lengthOfLeaveInBytes = ByteBuffer.wrap(lengthInByte, 0, 4).getInt();
                    if (lengthOfLeaveInBytes == 0) {
                        break;
                    }
                    byte[] leafInByte = new byte[lengthOfLeaveInBytes];
                    file.seek(offset + 4);
                    file.read(leafInByte);
                    BTreeLeafNode deserializedLeaf = deserializationHelper.deserializeLeaf(leafInByte,
                            bTreeOder, counter);
                    ArrayList<byte[]> tuples = deserializedLeaf.rangeSearchAndGetTuples(leftKey, rightKey);
                    if (tuples.size() == 0) {
                        break;
                    } else {
                        serializedTuples.addAll(tuples);
                    }
                    offset = offset + lengthOfLeaveInBytes + 4;

                }*/
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
//        }
//        System.out.println("The tuples are");
//        System.out.println(serializedTuples);
//        System.out.println("Size " + serializedTuples.size());
//        if (serializedTuples.size() != 0) {
            collector.emit(NormalDistributionIndexingTopology.FileSystemQueryStream,
                    new Values(queryId, serializedTuples));
//        }
//        collector.ack(tuple);
    }

    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
//        outputFieldsDeclarer.declareStream(NormalDistributionIndexingTopology.FileSystemQueryStream,
//                new Fields("leftKey", "rightKey", "serializedTuples"));
        outputFieldsDeclarer.declareStream(NormalDistributionIndexingAndRangeQueryTopology.FileSystemQueryStream,
                new Fields("queryId", "serializedTuples"));
    }

}
