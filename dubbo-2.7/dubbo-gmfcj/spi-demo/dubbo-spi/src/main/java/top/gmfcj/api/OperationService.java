package top.gmfcj.api;

import org.apache.dubbo.common.extension.SPI;

@SPI
public interface OperationService {

    public void operation();
}
