package top.gmfcj.lettcode;

/**
 * @description: 单链表实现
 */
public class ListNode {

    private Node head;
    private Node next;

    public ListNode() {

    }

    public ListNode(int[] arr) {
        Node curr = head;
        while (curr!= null && curr.hashNext()) {
            curr = curr.next;
        }
        for (int i = 0; i < arr.length; i++) {
            Node node = new Node(arr[i]);
            if (curr == null) {
                curr = head = node;
                continue;
            }
            curr.next = node;
            curr = node;
        }
    }

    public ListNode(int headVal) {
        if (head != null) {
            throw new IllegalArgumentException("节点已经初始化");
        }
        head = new Node(headVal);
    }

    public void add(int val) {
        if (head == null) {
            head = new Node(val);
        } else {
            Node curr = head;
            while (curr.hashNext()) {
                curr = curr.next;
            }
            curr.next = new Node(val);
        }
    }

    public void show() {
        Node cur = head;
        System.out.print("[ ");
        System.out.print("" + cur.value);
        while (cur.hashNext()) {
            cur = cur.next;
            System.out.print(", " + cur.value);
        }
        System.out.print(" ]");
    }


    public void reverse(){
        Node pointer = head;
        Node curr = null;
        Node prev = null;
        while (pointer != null){
            curr = pointer;
            pointer = pointer.next;
            // reverse the link
            curr.next = prev;
            prev = curr;
            head = curr;
        }
    }

    public Node getHead() {
        return head;
    }

    public void setHead(Node head) {
        this.head = head;
    }

    public Node getNext() {
        return next;
    }

    public void setNext(Node next) {
        this.next = next;
    }

    class Node {
        private int value;
        private Node next;

        public boolean hashNext() {
            return next != null;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public Node getNext() {
            return next;
        }

        public void setNext(Node next) {
            this.next = next;
        }

        public Node(int value) {
            this.value = value;
        }
    }
}
