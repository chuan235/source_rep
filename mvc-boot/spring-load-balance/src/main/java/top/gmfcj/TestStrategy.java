package top.gmfcj;

import top.gmfcj.circulate.CircullateStrategyV1;
import top.gmfcj.circulate.CircullateStrategyV3;
import top.gmfcj.random.RandomStrategyV1;
import top.gmfcj.random.RandomStrategyV2;

/**
 * @description: 测试负载均衡策略
 */
public class TestStrategy {

    public static void main(String[] args) {

        for (int i = 0; i < 20; i++) {
            //System.out.println(RandomStrategyV1.getServer());
//            System.out.println(RandomStrategyV2.getServer1());
//            System.out.println(RandomStrategyV2.getServer2());
//            System.out.println(CircullateStrategyV1.getServer());
            System.out.println(CircullateStrategyV3.getServer());
        }

    }

}
