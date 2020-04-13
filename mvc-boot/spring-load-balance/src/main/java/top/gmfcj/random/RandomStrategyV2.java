package top.gmfcj.random;

import top.gmfcj.ServerData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @description: 根据权重来进行随机
 * 1、根据权重生成不同数量的服务器ip，在从这写ip中随机获取
 * 2、去一个权重总数之间的数，根据权重总数获取到对应的ip
 */
public class RandomStrategyV2 {

    public static String getServer1() {
        // 根据权重生成不同数量的服务器ip
        List<String> fullList = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : ServerData.SERVER_WEIGHT.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                fullList.add(entry.getKey());
            }
        }
        // 获取总的权重值，也就是fullList的size
        int total = fullList.size();
        int index = (int) (Math.random() * total);
        return fullList.get(index);
    }

    public static String getServer2() {
        // true表示所有服务器的权重相同，直接随机取一个服务器返回
        boolean sameWeight = true;
        // 获取总的权重值
        int totalWeight = 0;
        // 创建一个数组存储所有的权重值
        Object[] weightArr = ServerData.SERVER_WEIGHT.values().toArray();
        // 判断权重是否相等
        for (int i = 0; i < weightArr.length; i++) {
            Integer weight = (Integer) weightArr[i];
            totalWeight += weight;
            if(sameWeight && i > 0 && !weight.equals(weightArr[i-1])){
                sameWeight = false;
            }
        }
        Random random = new Random();
        int position = random.nextInt(totalWeight);
        if(!sameWeight){
            for (String ip : ServerData.SERVER_WEIGHT.keySet()) {
                Integer value = ServerData.SERVER_WEIGHT.get(ip);
                if(position <= value){
                    return ip;
                }else{
                    position -= value;
                }
                
            }
        }
        // 这里随机从map中返回一个ip
        return (String) ServerData.SERVER_WEIGHT.keySet().toArray()[new Random().nextInt(ServerData.SERVER_WEIGHT.size())];
    }

    public static int getTotalWeight(){
        int total = 0;
        for (Integer weight : ServerData.SERVER_WEIGHT.values()) {
            total += weight;
        }
        return total;
    }

}
