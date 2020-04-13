package top.gmfcj.circulate;

import top.gmfcj.ServerData;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @description: 基本轮询
 */
public class CircullateStrategyV1 {

    private static AtomicInteger currentIndex = new AtomicInteger(0);

    public static String getServer(){
        int total = ServerData.SERVER_LIST.size();
        if(currentIndex.get() == total){
            currentIndex.set(0);
        }
        return ServerData.SERVER_LIST.get(currentIndex.getAndIncrement());
    }
}
