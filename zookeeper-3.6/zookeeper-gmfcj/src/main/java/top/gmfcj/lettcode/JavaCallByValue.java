package top.gmfcj.lettcode;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @description: Java中都是值传递的证明
 */
public class JavaCallByValue {


    public static void main(String[] args) throws IOException {
//        Student a = new Student("小A");
//        Student b = new Student("小B");
//        swap(a, b);
//        System.out.println("studentx.name = " + a.name);
//        System.out.println("studenty.name = " + b.name);
        ServerSocket serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress(9000));
        Socket as = serverSocket.accept();


        //        int a = 10;
//        int b = 20;
//        swap(a, b);
//        System.out.println("x.value = " + a);
//        System.out.println("y.value = " + b);
    }

    /**
     * @param x 是a的副本，它只是在这个方法中用
     * @param y 同x
     */
    public static void swap(Student x, Student y) {
        Student temp;
        temp = x;
        x = y;
        y = temp;
        // x.name = "tom";  找到地址修改值
        System.out.println("studentx.name = " + x.name);
        System.out.println("studenty.name = " + y.name);
    }


    public static void swap(int x, int y) {
//        int temp;
//        temp = x;
//        x = y;
//        y = temp;
        x += 20;
        y -= 20;
        System.out.println("x.value = " + x);
        System.out.println("y.value = " + y);
    }
}

class Student {
    public Student(String name) {
        this.name = name;
    }

    String name;
}