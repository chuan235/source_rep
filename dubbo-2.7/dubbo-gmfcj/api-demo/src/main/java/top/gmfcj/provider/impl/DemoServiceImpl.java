package top.gmfcj.provider.impl;

import top.gmfcj.api.DemoService;

public class DemoServiceImpl implements DemoService {

    @Override
    public String sayHello(String name) {
        return "hello DemoService Impl";
    }
}
