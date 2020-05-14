package top.gmfcj.jvmm;


import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class JVMMonitorTest {

    private JVMMonitorTest(){}

    private static AtomicReference<JVMMonitorTest> INSTANCE = new AtomicReference<>();

    private static ThreadLocal<JVMMonitorTest> localMonitor = new ThreadLocal<JVMMonitorTest>(){
        @Override
        protected JVMMonitorTest initialValue() {
            return new JVMMonitorTest();
        }
    };

    public static void main(String[] args) {
//        new Thread(new JVMMonitor(500, 3000, 1000)).start();

//        ThreadUtil.sleep(4000);
//        System.out.println("程序结束了");
//        System.exit(0);

        System.out.println(LocalDate.now().toString());
    }

    public static JVMMonitorTest getInstance(){
        for(;;){
            if(INSTANCE.get() != null){
                return INSTANCE.get();
            }
            JVMMonitorTest instance = new JVMMonitorTest();
            if(INSTANCE.compareAndSet(null, instance)){
                return instance;
            }
        }
    }

//    public static JVMMonitorTest getInstance2(){
//    }
}

class JVMMonitor implements Runnable {

    private volatile boolean shouldRun = true;

    protected long sleepTimeMs;
    protected long warnThresholdMs;
    protected long infoThresholdMs;

    public JVMMonitor(long sleepTimeMs, long warnThresholdMs, long infoThresholdMs) {
        this.sleepTimeMs = sleepTimeMs;
        this.warnThresholdMs = warnThresholdMs;
        this.infoThresholdMs = infoThresholdMs;
    }

    private long numGcWarnThresholdExceeded = 0;
    private long numGcInfoThresholdExceeded = 0;
    private long totalGcExtraSleepTime = 0;

    @Override
    public void run() {
        Map<String, GcTimes> gcTimesBeforeSleep = getGcTimes();
        while (shouldRun) {
            System.out.println("start while: numGcWarnThresholdExceeded =  " + numGcWarnThresholdExceeded);
            System.out.println("start while: numGcInfoThresholdExceeded =  " + numGcInfoThresholdExceeded);
            System.out.println("start while: totalGcExtraSleepTime =  " + totalGcExtraSleepTime);
            long startTime = Instant.now().toEpochMilli();
            try {
                Thread.sleep(sleepTimeMs);
            } catch (InterruptedException ie) {
                return;
            }
            long endTime = Instant.now().toEpochMilli();
            long extraSleepTime = (endTime - startTime) - sleepTimeMs;
            Map<String, GcTimes> gcTimesAfterSleep = getGcTimes();

            if (extraSleepTime > warnThresholdMs) {
                ++numGcWarnThresholdExceeded;
            } else if (extraSleepTime > infoThresholdMs) {
                ++numGcInfoThresholdExceeded;
            }
            totalGcExtraSleepTime += extraSleepTime;
            gcTimesBeforeSleep = gcTimesAfterSleep;
        }
    }

    private Map<String, GcTimes> getGcTimes() {
        Map<String, GcTimes> map = new HashMap<>();
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            map.put(gcBean.getName(), new GcTimes(gcBean));
        }
        return map;
    }

    private static class GcTimes {

        private long gcCount;
        private long gcTimeMillis;

        private GcTimes(GarbageCollectorMXBean gcBean) {
            gcCount = gcBean.getCollectionCount();
            gcTimeMillis = gcBean.getCollectionTime();
        }

        private GcTimes(long count, long time) {
            this.gcCount = count;
            this.gcTimeMillis = time;
        }

        private GcTimes subtract(GcTimes other) {
            return new GcTimes(this.gcCount - other.gcCount, this.gcTimeMillis - other.gcTimeMillis);
        }

        public String toString() {
            return "count=" + gcCount + " time=" + gcTimeMillis + "ms";
        }

    }
}