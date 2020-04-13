package top.gmfcj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @description: 模拟服务器数据
 */
public class ServerData {

    /**
     * 存储服务器地址
     */
    public final static List<String> SERVER_LIST = new ArrayList<>();

    /**
     * 存储服务器地址和其对应的权重
     */
    public final static Map<String,Integer> SERVER_WEIGHT = new HashMap<>();

    /**
     * 服务某一时刻的活跃数
     */
    public final static Map<String,Integer> ACTIVITY_MAP = new HashMap<>();


    public static Integer sequenceNum = 0;
    static {
        SERVER_LIST.add("192.168.137.100");
        SERVER_LIST.add("192.168.137.101");
        SERVER_LIST.add("192.168.137.102");
        SERVER_LIST.add("192.168.137.103");
        SERVER_LIST.add("192.168.137.104");
        SERVER_LIST.add("192.168.137.105");
        SERVER_LIST.add("192.168.137.106");
        SERVER_LIST.add("192.168.137.107");
        SERVER_LIST.add("192.168.137.108");
        SERVER_LIST.add("192.168.137.109");
        // 初始化权重数据
        SERVER_WEIGHT.put("192.168.137.100",3);
        SERVER_WEIGHT.put("192.168.137.101",7);
        SERVER_WEIGHT.put("192.168.137.102",2);
        SERVER_WEIGHT.put("192.168.137.103",8);
        SERVER_WEIGHT.put("192.168.137.104",4);
        SERVER_WEIGHT.put("192.168.137.105",6);
        SERVER_WEIGHT.put("192.168.137.106",5);
        SERVER_WEIGHT.put("192.168.137.107",5);
        SERVER_WEIGHT.put("192.168.137.108",1);
        SERVER_WEIGHT.put("192.168.137.109",9);


        SERVER_WEIGHT.put("A",5);
        SERVER_WEIGHT.put("B",2);
        SERVER_WEIGHT.put("C",2);

        ACTIVITY_MAP.put("192.168.137.100", 2);
        ACTIVITY_MAP.put("192.168.137.101", 3);
        ACTIVITY_MAP.put("192.168.137.102", 5);
        ACTIVITY_MAP.put("192.168.137.103", 3);
        ACTIVITY_MAP.put("192.168.137.104", 7);
        ACTIVITY_MAP.put("192.168.137.105", 1);
        ACTIVITY_MAP.put("192.168.137.106", 0);
        ACTIVITY_MAP.put("192.168.137.107", 6);
        ACTIVITY_MAP.put("192.168.137.108", 5);
        ACTIVITY_MAP.put("192.168.137.109", 9);
    }

    public static int getSequence(){
        return ++sequenceNum;
    }
}
