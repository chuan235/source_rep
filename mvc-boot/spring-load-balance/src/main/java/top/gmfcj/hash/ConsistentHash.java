package top.gmfcj.hash;

import top.gmfcj.ServerData;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @description: 一致性hash算法
 * 根据客户端的ip地址或者请求URL或者请求参数...进行hash算法，特别的对于同一个ip地址或者同一个url请求路径或者参数，hash出现的结果时相同的
 * hash得到的结果去对应一个虚拟节点，这个虚拟节点会归属到一个真实的节点下面
 * 好处：
 * 让每一个节点获得的请求更加平均
 * 如果直接hash到真实的节点上，极有可能出现hash值的分散不均
 * 或者当一个节点宕机时，相邻的一个节点的压力会突然变大，容易导致服务器级联故障
 */
public class ConsistentHash {

    /**
     * 存储虚拟节点环
     */
    private static SortedMap<Integer, String> virtualNodes = new TreeMap<>();

    private final static int VIRTUAL_NUM = 160;

    static {
        // 构建虚拟节点，每一个真实的节点都对应相同的虚拟节点
        for (int i = 0; i < ServerData.SERVER_LIST.size(); i++) {
            for (int j = 0; j < VIRTUAL_NUM; j++) {
                String ip = ServerData.SERVER_LIST.get(i);
                // 获取每一个虚拟节点的hash值
                int hash = getHash(ip + "VM" + j);
                virtualNodes.put(hash, ip);
            }
        }
    }
    public static String getServer(String client) {
        int hash = getHash(client);
        // 得到大于hash值的排序好的map
        SortedMap<Integer, String> tailMap = virtualNodes.tailMap(hash);
        // 获取大于该hash值的第一个节点位置
        Integer nodeIndex = tailMap.firstKey();
        // 如果不存在该nodeIndex，则返回根元素
        if(nodeIndex == null){
            nodeIndex = virtualNodes.firstKey();
        }
        // 返回对应的节点ip ip
        return tailMap.get(nodeIndex);
    }

    public static int getHash(String ip) {
        final int p = 16777619;
        int hash = (int) 2166136261L;
        for (int i = 0; i < ip.length(); i++)
            hash = hash ^ ip.charAt(i) * p;
        hash += hash << 13;
        hash ^= hash << 7;
        hash += hash << 3;
        hash ^= hash << 17;
        hash += hash << 5;
        if (hash < 0)
            hash = Math.abs(hash);
        return hash;
    }
}
