package top.gmfcj.circulate;

import top.gmfcj.ServerData;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @description: 轮询取余法
 */
public class CircullateStrategyV2 {

    private static Integer pos = 0;

    public static String getServer() {
        int totalWeight = 0;
        // 服务器的权重是否相等，如果相等，直接进行轮询返回服务器ip
        boolean sameWeight = true;
        Object[] weights = ServerData.SERVER_WEIGHT.values().toArray();
        for (int i = 0; i < weights.length; i++) {
            Integer value = (Integer) weights[i];
            totalWeight += value;
            if (sameWeight && i > 0 && !value.equals(weights[i - 1])) {
                sameWeight = false;
            }
        }
        int sequence = ServerData.getSequence();
        int index = sequence % totalWeight;
        index = index == 0 ? totalWeight : index;
        if (!sameWeight) {
            // 权重不同
            for (String ip : ServerData.SERVER_WEIGHT.keySet()) {
                Integer weight = ServerData.SERVER_WEIGHT.get(ip);
                if (sequence < weight) {
                    return ip;
                }
                index -= weight;
            }
        }
        String returnIp = "";
        // 权重相同，使用普通的轮询算法
        synchronized (pos) {
            if (pos >= ServerData.SERVER_WEIGHT.size()) {
                pos = 0;
            }
            returnIp = (String) ServerData.SERVER_WEIGHT.keySet().toArray()[pos];
            pos++;
        }
        return returnIp;
    }
}
