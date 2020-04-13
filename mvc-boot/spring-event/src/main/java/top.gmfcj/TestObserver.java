package top.gmfcj;


import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import top.gmfcj.java.Observer1;
import top.gmfcj.java.Observer2;
import top.gmfcj.java.Subject;
import top.gmfcj.self.Movie;
import top.gmfcj.self.MovieCryObserver;
import top.gmfcj.self.MovieLaughObserver;
import top.gmfcj.spring.AppConfig;
import top.gmfcj.spring.EmailService;
import top.gmfcj.spring.SpringListener;

import java.util.ArrayList;
import java.util.List;

public class TestObserver {

    public static void main(String[] args) {
//        testSpring();
//        testJava();
        testSelf();
    }

    public static void testSpring(){
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(AppConfig.class);
        context.refresh();

        List<String> blackList = new ArrayList<>();
        blackList.add("123@qq.com");
        blackList.add("gc@qq.com");

        EmailService service = context.getBean(EmailService.class);
        service.setBlackList(blackList);
        service.sendEmail("15950330095@qq.com", "hello world");
        service.sendEmail("123@qq.com", "hello 123");
        service.sendEmail("gc@qq.com", "hello gc");

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 测试java中的观察者模式
     */
    public static void testJava(){
        Subject subject = new Subject();
        subject.addObserver(new Observer1());
        subject.addObserver(new Observer2());
        subject.publish();
        subject.setData(435346);
        subject.setData(31);
    }
    /**
     * 测试java中的观察者模式
     */
    public static void testSelf(){
        Movie source = new Movie();
        source.add(new MovieCryObserver());
        source.add(new MovieLaughObserver());
        source.start();
        source.cry();
        source.laugh();
        source.end();
    }
}
