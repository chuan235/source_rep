package top.gmfcj.distribute.config;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.retry.RetryForever;
import org.apache.zookeeper.CreateMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {

    public static void main(String[] args) throws Exception {
        Config config = new Config();
        // 模拟一个配置项，实际生产中会在系统初始化时从配置文件中加载进来
        config.saveConfig("timeout", "1000");
        // 每3S打印一次获取到的配置项
        for (int i = 0; i < 10; i++) {
            System.out.println(config.getConfig("timeout"));
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    Map<String, String> cache = new HashMap<>();
    public static final String ZKSERVER = "192.168.222.129:2181";
    public static final String CONFIG = "/config";

    public CuratorFramework client;

    public Config() {
        try {
            client = CuratorFrameworkFactory.newClient(ZKSERVER, new RetryForever(3));
            client.start();
            // 创建根节点
            client.create().forPath(CONFIG,"config".getBytes());
            // 拉取配置中心的配置,初始化缓存
            List<String> list = client.getChildren().forPath(CONFIG);
            for (String name : list) {
                String value = new String(client.getData().forPath(CONFIG + "/" + name));
                cache.put(name, value);
            }
            // 添加监听器
            PathChildrenCache watcher = new PathChildrenCache(client, CONFIG, true);
            watcher.getListenable().addListener(new PathChildrenCacheListener() {
                @Override
                public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
                    System.out.println("pathChildrenCacheEvent.getData() = " + event.getData());
                    // 修改数据的path
                    String path = event.getData().getPath();
                    if (path.startsWith(CONFIG)) {
                        String name = path.replace(CONFIG + "/", "");
                        // 监听子节点修改
                        if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_UPDATED)) {
                            // 更新缓存中的数据
                            cache.put(name, new String(event.getData().getData()));
                        }
                        // 监听子节点删除
                        if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_REMOVED)) {
                            // 移除缓存中的数据
                            cache.remove(name);
                        }
                    }
                }
            });
            watcher.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取配置信息
     */
    public String getConfig(String key) throws Exception {
        // 缓存
        String value = cache.get(key);
        if (value == null) {
            String fullPath = getPath(key);
            if (client.checkExists().forPath(fullPath) == null) {
                return null;
            } else {
                value = new String(client.getData().forPath(fullPath));
                cache.put(key, value);
            }
        }
        // 通过zk获取
        return value;
    }

    /**
     * 保存配置信息
     */
    public boolean saveConfig(String key, String value) throws Exception {
        // 添加到zk
        String fullPath = getPath(key);
        if (cache.get(key) == null) {
            // 创建配置节点
            client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(fullPath, value.getBytes());
        } else if (value.equals(cache.get(key))) {
            // 和缓存中相等
            return true;
        } else {
            // 和缓存中不相等
            client.setData().forPath(fullPath, value.getBytes());
        }
        // 缓存修改或者新建的数据
        cache.put(key, value);
        return true;
    }

    private String getPath(String key) {
        return CONFIG + "/" + key;
    }
}
