package top.gmfcj.impl;

import top.gmfcj.api.OperationService;

public class PlusOperationService implements OperationService {
    @Override
    public void operation() {
        System.out.println("plus operation ");
    }
}
