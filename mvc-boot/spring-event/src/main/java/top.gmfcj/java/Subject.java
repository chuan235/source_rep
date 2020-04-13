package top.gmfcj.java;

import java.util.Observable;

/**
 * 使用这个类发布主题主题消息，就是唤醒观察者
 */
public class Subject extends Observable {

    public final static Integer ODD = 1;
    public final static Integer EVEN = 2;

    public void setData(int i){
        Integer flag = EVEN;
        if((i & 0x0001) == 1){
            // 基数
            flag = ODD;
        }
        setChanged();
        notifyObservers(flag);
    }

    public void publish(){
        System.out.println("开始发布事件");
        setChanged();
        notifyObservers();
    }

}
