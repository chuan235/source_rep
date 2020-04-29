package top.gmfcj.proxy;

import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

public class AsyncFactory {

    private static ExecutorService executorService = ThreadPool.getThreadPool();

    public static void syncCallLog(String title, String uri,String operType) {
        // 打日志
        executorService.execute(() -> {
            // 将数据写入数据库
        });
    }
}
