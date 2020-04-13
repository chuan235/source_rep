package top.gmfcj.active;

import top.gmfcj.ServerData;

import java.util.*;

/**
 * @description: 最小活跃数的负载均衡算法
 * 最小活跃数：
 * 每一个服务提供者对应了一个活跃数，初始情况下，每一个服务的活跃数都为0
 * 每次收到一个请求，就将这个活跃数加1，完成请求后将活跃数减1
 * 如果服务器的性能非常好，那么它的活跃数变化速度是很快的，因此活跃数的下降速度也比其他性能一般的服务器要快
 * 此时，这样的服务器就可以优先的获取到新的请求任务，这就是最小活跃负载均衡算法的基本思想
 * 最小活跃算法中还加入了权重值，就是当很多优秀的服务器在某一时刻的最小活跃数相同，这时就需要根据它们的权重去分发请求
 * 权重越大越有机会获得新的请求
 */
public class LeastActive {

    public static String getServer() {
        // 获取活跃值最小的服务器
        Optional<Integer> minValue = ServerData.ACTIVITY_MAP.values().stream().min(Comparator.naturalOrder());
        List<String> minActivityIps = new ArrayList<>();
        if (minValue.isPresent()) {
            // 存在多个活跃值相同的服务
            ServerData.ACTIVITY_MAP.forEach((ip, activity) -> {
                if (activity.equals(minValue.get())) {
                    minActivityIps.add(ip);
                }
            });
        }
        Map<String, Integer> weightList = new LinkedHashMap<>();
        if (minActivityIps.size() > 1) {
            // 获取权重进行选择
            // 获取权重
            ServerData.SERVER_WEIGHT.forEach((ip, weight) -> {
                if (minActivityIps.contains(ip)) {
                    weightList.put(ip, weight);
                }
            });
        }
        // 检查权重值
        int totalWeight = 0;
        boolean sameWeight = true;
        Object[] weights = weightList.values().toArray();
        for (int i = 0; i < weights.length; i++) {
            Integer weight = (Integer) weights[i];
            totalWeight += weight;
            if (sameWeight && i > 0 && !weight.equals(weights[i - 1])) {
                sameWeight = false;
            }
        }
        if (!sameWeight) {
            // 如果权重不相等，随机去一个值找它的位置
            int position = new Random().nextInt(totalWeight);
            for (Map.Entry<String, Integer> entry : weightList.entrySet()) {
                if (position <= entry.getValue()) {
                    return entry.getKey();
                }
                position -= entry.getValue();
            }
        }
        // 权重相等，随机获取一个ip返回
        return (String) weightList.keySet().toArray()[new Random().nextInt(weightList.size())];
    }
}
