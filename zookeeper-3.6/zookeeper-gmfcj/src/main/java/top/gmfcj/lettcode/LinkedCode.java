package top.gmfcj.lettcode;

import java.util.LinkedList;

/**
 * @description: 链表算法
 * https://yq.aliyun.com/articles/646175
 * <p>
 * 1、如何在一次传递中找到单链表的中间元素？  不知道链表的长度
 * 2、如何检查给定的链表是否包含循环？如何找到循环的起始节点？
 * 3、如何反转链表？
 * 4、在没有递归的情况下如何反转单链表？
 * 5、如何删除乱序链表中的重复节点？
 * 6、如何测量单链表的长度？
 * 7、如何从单链表的末端找出第三个节点？
 * 8、如何使用堆栈算出两个链表的总和？
 */
public class LinkedCode<E> {

    public static void main(String[] args) {
        LinkedCode code = new LinkedCode();
        int[] arr = {1, 2, 3, 4, 5, 67, 8, 9, 12, 15, 99};
        ListNode node = new ListNode(arr);
        node.show();
        code.findElement(node, 3);
        code.findMiddleElement(node);
        LinkedList linkedList = new LinkedList();
    }
// 把链表的数据拿出来放入数组中，反过来循环一次，设置值.如果数据很大
    // 针对单链表  ->   在遍历的时候构建一个新链表  替换掉原来的链表
    // 双端、双向和单链表一致   将前一个元素缓存起来  两个元素交换位置  一直循环下去

    /**
     * q1 : 如何在一次传递中找到单链表的中间元素   -->  使用快慢指针
     *
     * @param list
     */
    public void findMiddleElement(ListNode list) {
        ListNode.Node slow = list.getHead();
        ListNode.Node fast = list.getHead();
        while (fast != null && fast.getNext() != null) {
            slow = slow.getNext();
            fast = fast.getNext().getNext();
        }
        // slow and slow+1
        System.out.println("middle element is " + slow.getValue());
    }

    /**
     * 一次循环算出倒数第几个的元素
     *
     * @param index
     * @return
     */
    public void findElement(ListNode list, int index) {
        ListNode.Node slow = list.getHead();
        ListNode.Node fast = list.getHead();
        // 指到指定位置
        for (int i = 0; i < index - 1; i++) {
            fast = fast.getNext();
        }
        // 一起依次向后移
        while (fast != null && fast.getNext() != null) {
            slow = slow.getNext();
            fast = fast.getNext();
        }
        System.out.println("target element is " + slow.getValue());
    }

    /**
     * 删除链表中重复的节点
     *
     * @param listNode
     */
    public void deleteReplicatElement(ListNode listNode) {
        // 找到重复的节点
        int bitSet = 0;
        ListNode.Node curr = listNode.getHead();
        ListNode.Node prev = null;
        while (curr != null && curr.hashNext()) {
            // 判断是否重复
            int value = curr.getValue();
            if((bitSet ^ (1 << value)) == 1){
                // 重复
                prev.setNext(curr.getNext());
                continue;
            }
            bitSet |= (1 << value);
            prev = curr;
            curr = curr.getNext();
        }
    }


}
