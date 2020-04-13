package top.gmfcj.java;

import java.util.Observable;
import java.util.Observer;

public class Observer2 implements Observer {

    @Override
    public void update(Observable o, Object arg) {
        // 两个
        System.out.println(o.countObservers());
        // true
        System.out.println(o instanceof Subject);
        System.out.println("Observer2 ...");
        if(arg.equals(Subject.ODD)){
            System.out.println("observer2 观察到了偶数...data="+arg);
        }
    }
}
