package top.gmfcj.proxy;

import java.util.concurrent.*;

/**
 * 线程池
 */
public final class ThreadPool {

//    private int corePoolSize = 5;
//    private int maxPoolSize = 20;
//    private int queueCapacity = 100;
//    private int keepAliveSeconds = 300;
    private static ExecutorService threadPool;

    public static ExecutorService getThreadPool(){
        if(threadPool == null){
            synchronized (threadPool){
                if(threadPool == null) {
                    threadPool = new ThreadPoolExecutor(5, 20, 300, TimeUnit.SECONDS, new ArrayBlockingQueue<>(50));
                }
                return threadPool;
            }
        }
        return threadPool;
    }


}
