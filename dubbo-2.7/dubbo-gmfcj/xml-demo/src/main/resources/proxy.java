/*
 * Decompiled with CFR.
 *
 * Could not load the following classes:
 *  com.alibaba.dubbo.rpc.service.EchoService
 *  org.apache.dubbo.common.bytecode.ClassGenerator
 *  org.apache.dubbo.common.bytecode.ClassGenerator$DC
 *  top.gmfcj.api.HelloService
 */
package org.apache.dubbo.common.bytecode;

import com.alibaba.dubbo.rpc.service.EchoService;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import org.apache.dubbo.common.bytecode.ClassGenerator;
import top.gmfcj.api.HelloService;
// org.apache.dubbo.common.bytecode.proxy0
public class proxy0
        implements ClassGenerator.DC,
        HelloService,
        EchoService {
    public static Method[] methods;
    private InvocationHandler handler;

    public void sayHello(String string) {
        Object[] arrobject = new Object[]{string};
        Object object = this.handler.invoke(this, methods[0], arrobject);
    }

    public void replyHello() {
        Object[] arrobject = new Object[]{};
        Object object = this.handler.invoke(this, methods[1], arrobject);
    }

    public Object $echo(Object object) {
        Object[] arrobject = new Object[]{object};
        Object object2 = this.handler.invoke(this, methods[2], arrobject);
        return object2;
    }

    public proxy0() {
    }

    public proxy0(InvocationHandler invocationHandler) {
        this.handler = invocationHandler;
    }
}
