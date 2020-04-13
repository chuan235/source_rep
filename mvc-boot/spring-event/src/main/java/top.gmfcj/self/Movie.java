package top.gmfcj.self;

import java.util.ArrayList;
import java.util.List;

/**
 * 被观察者
 */
public class Movie {

    List<MovieObserver> observerList = new ArrayList<>();

    public void add(MovieObserver movieObserver){
        this.observerList.add(movieObserver);
    }

    public void start(){
        System.out.println("电影开始了....");
//        for (MovieObserver observer : observerList) {
//            observer.feel("start");
//        }
    }

    public void cry(){
        System.out.println("感动人心的情节出现了 —_—");
        for (MovieObserver observer : observerList) {
            observer.feel(new SelfCryMovieEvent(this));
        }
    }

    public void laugh(){
        System.out.println("开心一刻 ^_^");
        for (MovieObserver observer : observerList) {
            observer.feel(new SelfLaughMovieEvent(this));
        }
    }
    public void end(){
        System.out.println("电影结束了.....");
//        for (MovieObserver observer : observerList) {
//            observer.feel("end");
//        }
    }

}
