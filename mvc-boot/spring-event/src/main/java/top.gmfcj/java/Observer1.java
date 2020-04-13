package top.gmfcj.java;

import java.util.Observable;
import java.util.Observer;

public class Observer1 implements Observer {

    @Override
    public void update(Observable o, Object arg) {
        System.out.println("Observer1 ...");
        if(arg.equals(Subject.EVEN)){
            System.out.println("observer2 观察到了基数... data="+arg);
        }
    }
}
