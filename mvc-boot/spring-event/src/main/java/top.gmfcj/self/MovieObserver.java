package top.gmfcj.self;

/**
 * 观察者接口
 */
public interface MovieObserver {

    /**
     * 看电影的感觉
     */
    void feel(SelfMovieEvent event);

}
