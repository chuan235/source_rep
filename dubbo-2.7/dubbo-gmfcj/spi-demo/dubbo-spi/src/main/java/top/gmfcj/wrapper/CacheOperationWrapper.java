package top.gmfcj.wrapper;

import top.gmfcj.api.OperationService;

public class CacheOperationWrapper implements OperationService {

    private OperationService operationService;

    public CacheOperationWrapper(OperationService operationService){
        this.operationService = operationService;
    }
    @Override
    public void operation() {
        System.out.println("缓存操作开始");
        operationService.operation();
        System.out.println("缓存操作结束");
    }
}
