package top.gmfcj.log;

import org.apache.ibatis.logging.Log;

import java.util.logging.Logger;

public class MyLog implements Log {
    public MyLog() {
    }

    private Logger logger;
    public MyLog(String s) {
        logger = Logger.getLogger(s);
    }
    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public boolean isTraceEnabled() {
        return false;
    }

    @Override
    public void error(String s, Throwable throwable) {
        logger.warning(s);
    }

    @Override
    public void error(String s) {
        logger.warning(s);
    }

    @Override
    public void debug(String s) {
        logger.info(s);
    }

    @Override
    public void trace(String s) {
       logger.config(s);
    }

    @Override
    public void warn(String s) {
        logger.warning(s);
    }


    @Override
    public String toString() {
        return "MyLog{}";
    }
}
