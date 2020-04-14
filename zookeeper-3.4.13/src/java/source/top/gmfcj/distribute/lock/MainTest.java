package top.gmfcj.distribute.lock;

/**
 * 数据库的行级锁 select for update
 *  1、行级锁
 *  2、where条件后需要写出明确的索引条件
 *  3、如果其所在的事务提交或者回滚后，或者更新该条数据后，那么会自动解锁
 * redis实现的分布式锁
 *
 * zookeeper实现的分布式锁
 *  1、创建客户端的时候为当前系统绑定一个临时的顺序节点（唯一和系统挂了节点会自己消失）
 *  2、尝试获取锁的时候需要判断当前系统对应的节点是不是第一顺位节点，最小的节点(这个规则可以自己制定)
 *  3、如果不是第一顺位节点，则会阻塞，并为前一个节点绑定一个节点移除的watch，当前节点释放锁时会删除临时节点，触发这里的watch
 *  4、释放锁的时候会删除当前系统对应的临时节点
 */
public class MainTest {

    public static void main(String[] args) {
        Ticket ticket = new Ticket();

        new Thread(new SystemOne(ticket)).start();
        new Thread(new SystemTwo(ticket)).start();

    }

}
