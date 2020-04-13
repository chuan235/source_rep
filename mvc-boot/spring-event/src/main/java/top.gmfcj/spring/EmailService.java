package top.gmfcj.spring;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailService implements ApplicationEventPublisherAware {

    @Value("#{'${email.black.list}'.split(',')}")
    private List<String> blackList;

    private ApplicationEventPublisher publisher;

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.publisher = applicationEventPublisher;
    }

    public void setBlackList(List<String> blackList) {
        this.blackList = blackList;
    }

    public void sendEmail(String address, String content){
        if (blackList.contains(address)) {
            System.out.println("目标邮箱地址已被拉入黑名单...address="+address);
        }else{
            // 如果名单中存在这个地址就发布发邮件的事件
            publisher.publishEvent(new BlackListEvent(this, address, content));
            return;
        }
    }
}
