/*
 * Decompiled with CFR.
 *
 * Could not load the following classes:
 *  org.apache.dubbo.common.URL
 *  org.apache.dubbo.common.extension.ExtensionLoader
 *  top.gmfcj.api.DemoService
 */
package top.gmfcj.api;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import top.gmfcj.api.DemoService;

public class DemoService$Adaptive
        implements DemoService {
    public String say(String string, URL uRL) {
        if (uRL == null) {
            throw new IllegalArgumentException("url == null");
        }
        URL uRL2 = uRL;
        String string2 = uRL2.getParameter("demo");
        if (string2 == null) {
            throw new IllegalStateException(new StringBuffer().append("Fail to get extension(top.gmfcj.api.DemoService) name from url(").append(uRL2.toString()).append(") use keys([demo])").toString());
        }
        DemoService demoService = (DemoService)ExtensionLoader.getExtensionLoader(DemoService.class).getExtension(string2);
        return demoService.say(string, uRL);
    }

    public void eat() {
        throw new UnsupportedOperationException("method public abstract void top.gmfcj.api.DemoService.eat() of interface top.gmfcj.api.DemoService is not adaptive method!");
    }
}
