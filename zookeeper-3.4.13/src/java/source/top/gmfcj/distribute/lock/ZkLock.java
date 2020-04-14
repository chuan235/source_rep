package top.gmfcj.distribute.lock;

import org.apache.zookeeper.*;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class ZkLock implements Lock {

    private static final String ZK_SERVER = "192.168.222.129:2181";
    private static final String LOCK_PATH = "/LOCK";
    private static final String LOCK_PERFIX = "/service_";
    private ThreadLocal<ZooKeeper> client = new ThreadLocal<>();
    private ThreadLocal<String> servicePath = new ThreadLocal<>();

    @Override
    public void lock() {
        init();
        tryLock();
    }

    public void init() {
        try {
            client.set(new ZooKeeper(ZK_SERVER, 200000, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    // ...
                }
            }));
            // 创建临时节点，返回的是临时节点的全路径 /LOCK/service_0001
            String path = client.get().create(LOCK_PATH + LOCK_PERFIX, new byte[12], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            // 存储当前节点的名称,全路径
            servicePath.set(path);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void unlock() {
        // 删除自己的节点
        try {
            client.get().delete(servicePath.get(), -1);
            System.out.println(Thread.currentThread().getName()+"释放锁成功...");
        } catch (Exception e) {
            System.out.println("释放锁失败：删除节点失败!");
        }
    }

    @Override
    public boolean tryLock() {
        // 判断自己线程中的节点是不是最小的节点
        try {
            // children[ service_xxx,...]
            List<String> children = client.get().getChildren(LOCK_PATH, false);
            Collections.sort(children);
            // 当前的节点是第一个
            if (servicePath.get().equals(LOCK_PATH + "/" + children.get(0))) {
                System.out.println(Thread.currentThread().getName() + "可以获取锁了" + servicePath.get());
                return true;
            } else {
                // 阻塞在这里
                CountDownLatch countDownLatch = new CountDownLatch(1);
                // 当前节点的索引
                int index = children.indexOf(servicePath.get().replace(LOCK_PATH + "/", ""));
                // 绑定一个前节点移除的监听器
                client.get().exists(LOCK_PATH + "/"+ children.get(index - 1), new Watcher() {
                    @Override
                    public void process(WatchedEvent event) {
                        // 前节点删除的时候
                        if (event.getType().equals(Event.EventType.NodeDeleted)) {
                            // 唤醒try
                            countDownLatch.countDown();
                        }
                    }
                });
                // 阻塞
                countDownLatch.await();
                System.out.println(Thread.currentThread().getName() + "等待到锁了" + servicePath.get());
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return false;
    }


    @Override
    public void lockInterruptibly() throws InterruptedException {

    }


    @Override
    public Condition newCondition() {
        return null;
    }
}
