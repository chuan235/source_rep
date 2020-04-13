package top.gmfcj.circulate;

import top.gmfcj.ServerData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @description: 平滑加权轮询算法
 * (1)当前权重     (2)选择当前权重的最大的值     (3)权重最大的要减去权重总和       (4)最终的权重加上最初的权重最为下一次的当前权重
 * [5,2,2]              A                           [-4,2,2]                        [1,4,4]
 * [1,4,4]              B                           [1,-5,4]                        [6,-3,6]
 * [6,-3,6]             A                           [-3,-3,6]                       [2,-1,8]
 * [2,-1,8]             C                           [2,-1,-1]                        [7,1,1]
 * [7,1,1]              A                           [-2,1,1]                        [3,3,3]
 * [3,3,3]	            A	                        [-6,3,3]	                    [-1,5,5]
 * [-1,5,5]             B 	                        [-1,-4,5]	                    [4,-2,7]
 * [4,-2,7]             c                           [4,-2,-2]	                    [9,0,0]
 * [9,0,0]              A                           [0,0,0]                         [5,2,2]
 * [5,2,2] ...
 */
public class CircullateStrategyV3 {

    public static List<WeightServer> serverList = new ArrayList<>();

    static {
        for (Map.Entry<String, Integer> entry : ServerData.SERVER_WEIGHT.entrySet()) {
            serverList.add(new WeightServer(entry.getKey(), entry.getValue(), entry.getValue()));
        }
    }

    public static String getServer() {
        // 找出权重最大的值
        WeightServer returnServer = new WeightServer("", 0, 0);
        int totalWeight = 0;
        for (WeightServer server : serverList) {
            // 获取当前的总权重
            totalWeight += server.getCurrentWeight();
            if (server.getCurrentWeight() > returnServer.getCurrentWeight()) {
                // 找出最大的权重
                returnServer = server;
            }
        }
        returnServer.setCurrentWeight(returnServer.getCurrentWeight() - totalWeight);
        String returnIp = returnServer.getIp();
        // 计算下一步的权重
        for (WeightServer server : serverList) {
            server.setCurrentWeight(server.getCurrentWeight() + server.getWeight());
        }
        return returnIp;
    }

    private static Map<String, WeightServer> weightMap = new HashMap<>();

    public static String getServer2() {
        // 初始化weightMap
        if (weightMap.isEmpty()) {
            ServerData.SERVER_WEIGHT.forEach((key, value) -> {
                weightMap.put(key, new WeightServer(key, value, value));
            });
        }
        // 获取总权重,最大权重的节点
        int totalWeight = 0;
        WeightServer maxWeightServer = null;
        for (WeightServer server : weightMap.values()) {
            totalWeight += server.getCurrentWeight();
            if (maxWeightServer == null || server.getCurrentWeight() > maxWeightServer.getCurrentWeight()) {
                maxWeightServer = server;
            }
        }
        // 计算新权重
        maxWeightServer.setCurrentWeight(maxWeightServer.getCurrentWeight() - totalWeight);
        // 计算所有的新权重
        for (WeightServer server : weightMap.values()) {
            server.setCurrentWeight(server.getCurrentWeight() + server.getWeight());
        }
        return maxWeightServer.getIp();
    }
}
