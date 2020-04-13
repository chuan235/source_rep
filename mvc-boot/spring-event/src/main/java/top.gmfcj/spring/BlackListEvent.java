package top.gmfcj.spring;

import org.springframework.context.ApplicationEvent;
import org.springframework.stereotype.Component;


public class BlackListEvent extends ApplicationEvent {

    private String url;
    private String content;

    public BlackListEvent(Object source,String url,String content) {
        super(source);
        this.url = url;
        this.content = content;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
