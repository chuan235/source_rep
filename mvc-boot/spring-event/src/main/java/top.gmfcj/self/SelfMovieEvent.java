package top.gmfcj.self;

import java.util.EventObject;

/**
 * 电影事件接口
 */
public class SelfMovieEvent extends EventObject {

    /**
     * 传入事件源
     * @param source
     */
    public SelfMovieEvent(Object source) {
        super(source);
    }
}
