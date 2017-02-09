package indexingTopology.config;

/**
 * Created by acelzj on 7/21/16.
 */
public class TopologyConfig {
    public static final double REBUILD_TEMPLATE_PERCENTAGE = 0.2;

    public static final String HDFS_HOST = "hdfs://192.168.0.237:54310/";

    public static final int NUMBER_TUPLES_OF_A_CHUNK = 600000;
//    public static final int NUMBER_TUPLES_OF_A_CHUNK = 200000;
    public static final int CACHE_SIZE = 10;

    public static final int TASK_QUEUE_CAPACITY = 10000;

    public static final int NUMBER_OF_INTERVALS = 100;

    public static final int BTREE_ORDER = 64;

    public static final double LOAD_BALANCE_THRESHOLD = 0.2;

    public static boolean HDFSFlag = false;

    public static String dataDir = "/home/acelzj";

    public static String dataFileDir = "/test_data/uniform_data.txt";

    public static String logDir = "/logs";

    public static double SKEWNESS_DETECTION_THRESHOLD = 0.6;

    public static final int PENDING_QUEUE_CAPACITY = 600005;

}
