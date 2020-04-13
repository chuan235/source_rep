package top.gmfcj.spring;

import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;


@Component
public class BlackListListener implements ApplicationListener<BlackListEvent> {

    @Override
    public void onApplicationEvent(BlackListEvent blackListEvent) {
        String url = blackListEvent.getUrl();
        String content = blackListEvent.getContent();
        System.out.println("正在发送邮件，地址是:" + url + "；内容为:" + content);
    }
}
