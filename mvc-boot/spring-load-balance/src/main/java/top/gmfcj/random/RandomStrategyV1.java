package top.gmfcj.random;

import top.gmfcj.ServerData;

import java.util.Random;

/**
 * @description: 最为简单的随机策略
 * 问题：
 *      不考虑机器性能，理论上是完全平均的
 */
public class RandomStrategyV1 {

    public static String getServer() {
        // 获取服务器总数
        int total = ServerData.SERVER_LIST.size();
        Random random = new Random();
        int index = (int) (Math.random() * total);
//        int index = random.nextInt(total);
        return ServerData.SERVER_LIST.get(index);
    }

}
