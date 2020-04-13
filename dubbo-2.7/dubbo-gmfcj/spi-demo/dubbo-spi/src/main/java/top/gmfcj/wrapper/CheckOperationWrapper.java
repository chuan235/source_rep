package top.gmfcj.wrapper;


import top.gmfcj.api.OperationService;

public class CheckOperationWrapper implements OperationService {

    private OperationService operationService;

    public CheckOperationWrapper(OperationService operationService){
        this.operationService = operationService;
    }
    @Override
    public void operation() {
        System.out.println("校验参数开始....");
        operationService.operation();
        System.out.println("校验参数结束....");
    }
}
