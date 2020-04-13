/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.rpc.protocol.dubbo;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.serialize.support.SerializableClassRegistry;
import org.apache.dubbo.common.serialize.support.SerializationOptimizer;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.Transporter;
import org.apache.dubbo.remoting.exchange.ExchangeChannel;
import org.apache.dubbo.remoting.exchange.ExchangeClient;
import org.apache.dubbo.remoting.exchange.ExchangeHandler;
import org.apache.dubbo.remoting.exchange.ExchangeServer;
import org.apache.dubbo.remoting.exchange.Exchangers;
import org.apache.dubbo.remoting.exchange.support.ExchangeHandlerAdapter;
import org.apache.dubbo.rpc.AsyncContextImpl;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.protocol.AbstractProtocol;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * dubbo protocol support.
 */
public class DubboProtocol extends AbstractProtocol {

    public static final String NAME = "dubbo";

    public static final int DEFAULT_PORT = 20880;
    private static final String IS_CALLBACK_SERVICE_INVOKE = "_isCallBackServiceInvoke";
    private static DubboProtocol INSTANCE;
    private final Map<String, ExchangeServer> serverMap = new ConcurrentHashMap<String, ExchangeServer>(); // <host:port,Exchanger>
    private final Map<String, ReferenceCountExchangeClient> referenceClientMap = new ConcurrentHashMap<String, ReferenceCountExchangeClient>(); // <host:port,Exchanger>
    private final ConcurrentMap<String, LazyConnectExchangeClient> ghostClientMap = new ConcurrentHashMap<String, LazyConnectExchangeClient>();
    private final ConcurrentMap<String, Object> locks = new ConcurrentHashMap<String, Object>();
    private final Set<String> optimizers = new ConcurrentHashSet<String>();
    //consumer side export a stub service for dispatching event
    //servicekey-stubmethods
    private final ConcurrentMap<String, String> stubServiceMethodsMap = new ConcurrentHashMap<String, String>();
    private ExchangeHandler requestHandler = new ExchangeHandlerAdapter() {

        @Override
        public CompletableFuture<Object> reply(ExchangeChannel channel, Object message) throws RemotingException {
            if (message instanceof Invocation) {
                Invocation inv = (Invocation) message;
                Invoker<?> invoker = getInvoker(channel, inv);
                // need to consider backward-compatibility if it's a callback
                if (Boolean.TRUE.toString().equals(inv.getAttachments().get(IS_CALLBACK_SERVICE_INVOKE))) {
                    String methodsStr = invoker.getUrl().getParameters().get("methods");
                    boolean hasMethod = false;
                    if (methodsStr == null || !methodsStr.contains(",")) {
                        hasMethod = inv.getMethodName().equals(methodsStr);
                    } else {
                        String[] methods = methodsStr.split(",");
                        for (String method : methods) {
                            if (inv.getMethodName().equals(method)) {
                                hasMethod = true;
                                break;
                            }
                        }
                    }
                    if (!hasMethod) {
                        logger.warn(new IllegalStateException("The methodName " + inv.getMethodName()
                                + " not found in callback service interface ,invoke will be ignored."
                                + " please update the api interface. url is:"
                                + invoker.getUrl()) + " ,invocation is :" + inv);
                        return null;
                    }
                }
                RpcContext rpcContext = RpcContext.getContext();
                boolean supportServerAsync = invoker.getUrl().getMethodParameter(inv.getMethodName(), Constants.ASYNC_KEY, false);
                if (supportServerAsync) {
                    CompletableFuture<Object> future = new CompletableFuture<>();
                    rpcContext.setAsyncContext(new AsyncContextImpl(future));
                }
                rpcContext.setRemoteAddress(channel.getRemoteAddress());
                Result result = invoker.invoke(inv);

                if (result instanceof AsyncRpcResult) {
                    return ((AsyncRpcResult) result).getResultFuture().thenApply(r -> (Object) r);
                } else {
                    return CompletableFuture.completedFuture(result);
                }
            }
            throw new RemotingException(channel, "Unsupported request: "
                    + (message == null ? null : (message.getClass().getName() + ": " + message))
                    + ", channel: consumer: " + channel.getRemoteAddress() + " --> provider: " + channel.getLocalAddress());
        }

        @Override
        public void received(Channel channel, Object message) throws RemotingException {
            if (message instanceof Invocation) {
                reply((ExchangeChannel) channel, message);
            } else {
                super.received(channel, message);
            }
        }

        @Override
        public void connected(Channel channel) throws RemotingException {
            invoke(channel, Constants.ON_CONNECT_KEY);
        }

        @Override
        public void disconnected(Channel channel) throws RemotingException {
            if (logger.isInfoEnabled()) {
                logger.info("disconnected from " + channel.getRemoteAddress() + ",url:" + channel.getUrl());
            }
            invoke(channel, Constants.ON_DISCONNECT_KEY);
        }

        private void invoke(Channel channel, String methodKey) {
            Invocation invocation = createInvocation(channel, channel.getUrl(), methodKey);
            if (invocation != null) {
                try {
                    received(channel, invocation);
                } catch (Throwable t) {
                    logger.warn("Failed to invoke event method " + invocation.getMethodName() + "(), cause: " + t.getMessage(), t);
                }
            }
        }

        private Invocation createInvocation(Channel channel, URL url, String methodKey) {
            String method = url.getParameter(methodKey);
            if (method == null || method.length() == 0) {
                return null;
            }
            RpcInvocation invocation = new RpcInvocation(method, new Class<?>[0], new Object[0]);
            invocation.setAttachment(Constants.PATH_KEY, url.getPath());
            invocation.setAttachment(Constants.GROUP_KEY, url.getParameter(Constants.GROUP_KEY));
            invocation.setAttachment(Constants.INTERFACE_KEY, url.getParameter(Constants.INTERFACE_KEY));
            invocation.setAttachment(Constants.VERSION_KEY, url.getParameter(Constants.VERSION_KEY));
            if (url.getParameter(Constants.STUB_EVENT_KEY, false)) {
                invocation.setAttachment(Constants.STUB_EVENT_KEY, Boolean.TRUE.toString());
            }
            return invocation;
        }
    };

    public DubboProtocol() {
        INSTANCE = this;
    }

    public static DubboProtocol getDubboProtocol() {
        if (INSTANCE == null) {
            ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(DubboProtocol.NAME); // load
        }
        return INSTANCE;
    }

    public Collection<ExchangeServer> getServers() {
        return Collections.unmodifiableCollection(serverMap.values());
    }

    public Collection<Exporter<?>> getExporters() {
        return Collections.unmodifiableCollection(exporterMap.values());
    }

    Map<String, Exporter<?>> getExporterMap() {
        return exporterMap;
    }

    private boolean isClientSide(Channel channel) {
        InetSocketAddress address = channel.getRemoteAddress();
        URL url = channel.getUrl();
        return url.getPort() == address.getPort() &&
                NetUtils.filterLocalHost(channel.getUrl().getIp())
                        .equals(NetUtils.filterLocalHost(address.getAddress().getHostAddress()));
    }

    Invoker<?> getInvoker(Channel channel, Invocation inv) throws RemotingException {
        boolean isCallBackServiceInvoke = false;
        boolean isStubServiceInvoke = false;
        int port = channel.getLocalAddress().getPort();
        String path = inv.getAttachments().get(Constants.PATH_KEY);
        // if it's callback service on client side
        isStubServiceInvoke = Boolean.TRUE.toString().equals(inv.getAttachments().get(Constants.STUB_EVENT_KEY));
        if (isStubServiceInvoke) {
            port = channel.getRemoteAddress().getPort();
        }
        //callback
        isCallBackServiceInvoke = isClientSide(channel) && !isStubServiceInvoke;
        if (isCallBackServiceInvoke) {
            path = inv.getAttachments().get(Constants.PATH_KEY) + "." + inv.getAttachments().get(Constants.CALLBACK_SERVICE_KEY);
            inv.getAttachments().put(IS_CALLBACK_SERVICE_INVOKE, Boolean.TRUE.toString());
        }
        String serviceKey = serviceKey(port, path, inv.getAttachments().get(Constants.VERSION_KEY), inv.getAttachments().get(Constants.GROUP_KEY));

        DubboExporter<?> exporter = (DubboExporter<?>) exporterMap.get(serviceKey);

        if (exporter == null) {
            throw new RemotingException(channel, "Not found exported service: " + serviceKey + " in " + exporterMap.keySet() + ", may be version or group mismatch " + ", channel: consumer: " + channel.getRemoteAddress() + " --> provider: " + channel.getLocalAddress() + ", message:" + inv);
        }

        return exporter.getInvoker();
    }

    public Collection<Invoker<?>> getInvokers() {
        return Collections.unmodifiableCollection(invokers);
    }

    @Override
    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    @Override
    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        URL url = invoker.getUrl();
        // export dubbo protocol service. 服务的唯一标识，由服务组名，服务名，服务版本号以及端口组成
        // demoGroup/com.alibaba.dubbo.demo.DemoService:1.0.1:20880 或者 top.gmfcj.api.HelloService:20890
        String key = serviceKey(url);
        DubboExporter<T> exporter = new DubboExporter<T>(invoker, key, exporterMap);
        exporterMap.put(key, exporter);

        //export an stub service for dispatching event 本地存根相关
        Boolean isStubSupportEvent = url.getParameter(Constants.STUB_EVENT_KEY, Constants.DEFAULT_STUB_EVENT);
        Boolean isCallbackservice = url.getParameter(Constants.IS_CALLBACK_SERVICE, false);
        if (isStubSupportEvent && !isCallbackservice) {
            String stubServiceMethods = url.getParameter(Constants.STUB_EVENT_METHODS_KEY);
            if (stubServiceMethods == null || stubServiceMethods.length() == 0) {
                if (logger.isWarnEnabled()) {
                    logger.warn(new IllegalStateException("consumer [" + url.getParameter(Constants.INTERFACE_KEY) +
                            "], has set stubproxy support event ,but no stub methods founded."));
                }
            } else {
                stubServiceMethodsMap.put(url.getServiceKey(), stubServiceMethods);
            }
        }
        // 启动服务器
        openServer(url);
        // 优化序列化
        optimizeSerialization(url);
        return exporter;
    }

    private void openServer(URL url) {
        // find server. 获取ip和端口 169.254.94.86:20890
        String key = url.getAddress();
        //client can export a service which's only for server to invoke
        boolean isServer = url.getParameter(Constants.IS_SERVER_KEY, true);
        if (isServer) {
            // 从缓存中获取
            ExchangeServer server = serverMap.get(key);
            if (server == null) {
                synchronized (this) {
                    server = serverMap.get(key);
                    if (server == null) {
                        // 创建服务器实例
                        serverMap.put(key, createServer(url));
                    }
                }
            } else {
                // server supports reset, use together with override 如果某个端口上已经存在某个服务器实例了，使用reset方法重置服务器的一些配置
                server.reset(url);
            }
        }
    }

    private ExchangeServer createServer(URL url) {
        // send readonly event when server closes, it's enabled by default
        url = url.addParameterIfAbsent(Constants.CHANNEL_READONLYEVENT_SENT_KEY, Boolean.TRUE.toString());
        // enable heartbeat by default 添加心跳检测配置到 url 中
        url = url.addParameterIfAbsent(Constants.HEARTBEAT_KEY, String.valueOf(Constants.DEFAULT_HEARTBEAT));
        // 获取server参数，默认的远程服务器类型是netty
        String str = url.getParameter(Constants.SERVER_KEY, Constants.DEFAULT_REMOTING_SERVER);
        // 通过 SPI 检测是否存在 server 参数所代表的 Transporter 拓展，不存在则抛出异常
        if (str != null && str.length() > 0 && !ExtensionLoader.getExtensionLoader(Transporter.class).hasExtension(str)) {
            throw new RpcException("Unsupported server type: " + str + ", url: " + url);
        }
        // 添加url编码解码参数
        url = url.addParameter(Constants.CODEC_KEY, DubboCodec.NAME);
        ExchangeServer server;
        try {
            // 创建 ExchangeServer
            server = Exchangers.bind(url, requestHandler);
        } catch (RemotingException e) {
            throw new RpcException("Fail to start server(url: " + url + ") " + e.getMessage(), e);
        }
        str = url.getParameter(Constants.CLIENT_KEY);
        if (str != null && str.length() > 0) {
            // 获取所有的 Transporter 接口的实现类
            Set<String> supportedTypes = ExtensionLoader.getExtensionLoader(Transporter.class).getSupportedExtensions();
            if (!supportedTypes.contains(str)) {
                // 如果没有支持当前 REMOTING_SERVER 会抛出异常
                throw new RpcException("Unsupported client type: " + str);
            }
        }
        return server;
    }

    private void optimizeSerialization(URL url) throws RpcException {
        String className = url.getParameter(Constants.OPTIMIZER_KEY, "");
        if (StringUtils.isEmpty(className) || optimizers.contains(className)) {
            return;
        }

        logger.info("Optimizing the serialization process for Kryo, FST, etc...");

        try {
            Class clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
            if (!SerializationOptimizer.class.isAssignableFrom(clazz)) {
                throw new RpcException("The serialization optimizer " + className + " isn't an instance of " + SerializationOptimizer.class.getName());
            }

            SerializationOptimizer optimizer = (SerializationOptimizer) clazz.newInstance();

            if (optimizer.getSerializableClasses() == null) {
                return;
            }

            for (Class c : optimizer.getSerializableClasses()) {
                SerializableClassRegistry.registerClass(c);
            }

            optimizers.add(className);
        } catch (ClassNotFoundException e) {
            throw new RpcException("Cannot find the serialization optimizer class: " + className, e);
        } catch (InstantiationException e) {
            throw new RpcException("Cannot instantiate the serialization optimizer class: " + className, e);
        } catch (IllegalAccessException e) {
            throw new RpcException("Cannot instantiate the serialization optimizer class: " + className, e);
        }
    }

    @Override
    public <T> Invoker<T> refer(Class<T> serviceType, URL url) throws RpcException {
        optimizeSerialization(url);
        // create rpc invoker.
        DubboInvoker<T> invoker = new DubboInvoker<T>(serviceType, url, getClients(url), invokers);
        invokers.add(invoker);
        return invoker;
    }

    private ExchangeClient[] getClients(URL url) {
        // whether to share connection
        boolean serviceShareConnect = false;
        int connections = url.getParameter(Constants.CONNECTIONS_KEY, 0);
        // if not configured, connection is shared, otherwise, one connection for one service
        if (connections == 0) {
            serviceShareConnect = true;
            connections = 1;
        }

        ExchangeClient[] clients = new ExchangeClient[connections];
        for (int i = 0; i < clients.length; i++) {
            if (serviceShareConnect) {
                clients[i] = getSharedClient(url);
            } else {
                clients[i] = initClient(url);
            }
        }
        return clients;
    }

    /**
     * Get shared connection
     */
    private ExchangeClient getSharedClient(URL url) {
        String key = url.getAddress();
        ReferenceCountExchangeClient client = referenceClientMap.get(key);
        if (client != null) {
            if (!client.isClosed()) {
                client.incrementAndGetCount();
                return client;
            } else {
                referenceClientMap.remove(key);
            }
        }

        locks.putIfAbsent(key, new Object());
        synchronized (locks.get(key)) {
            if (referenceClientMap.containsKey(key)) {
                return referenceClientMap.get(key);
            }

            ExchangeClient exchangeClient = initClient(url);
            client = new ReferenceCountExchangeClient(exchangeClient, ghostClientMap);
            referenceClientMap.put(key, client);
            ghostClientMap.remove(key);
            locks.remove(key);
            return client;
        }
    }

    /**
     * Create new connection
     */
    private ExchangeClient initClient(URL url) {

        // client type setting.
        String str = url.getParameter(Constants.CLIENT_KEY, url.getParameter(Constants.SERVER_KEY, Constants.DEFAULT_REMOTING_CLIENT));

        url = url.addParameter(Constants.CODEC_KEY, DubboCodec.NAME);
        // enable heartbeat by default
        url = url.addParameterIfAbsent(Constants.HEARTBEAT_KEY, String.valueOf(Constants.DEFAULT_HEARTBEAT));

        // BIO is not allowed since it has severe performance issue.
        if (str != null && str.length() > 0 && !ExtensionLoader.getExtensionLoader(Transporter.class).hasExtension(str)) {
            throw new RpcException("Unsupported client type: " + str + "," +
                    " supported client type is " + StringUtils.join(ExtensionLoader.getExtensionLoader(Transporter.class).getSupportedExtensions(), " "));
        }

        ExchangeClient client;
        try {
            // connection should be lazy
            if (url.getParameter(Constants.LAZY_CONNECT_KEY, false)) {
                client = new LazyConnectExchangeClient(url, requestHandler);
            } else {
                client = Exchangers.connect(url, requestHandler);
            }
        } catch (RemotingException e) {
            throw new RpcException("Fail to create remoting client for service(" + url + "): " + e.getMessage(), e);
        }
        return client;
    }

    @Override
    public void destroy() {
        for (String key : new ArrayList<String>(serverMap.keySet())) {
            ExchangeServer server = serverMap.remove(key);
            if (server != null) {
                try {
                    if (logger.isInfoEnabled()) {
                        logger.info("Close dubbo server: " + server.getLocalAddress());
                    }
                    server.close(ConfigurationUtils.getServerShutdownTimeout());
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            }
        }

        for (String key : new ArrayList<String>(referenceClientMap.keySet())) {
            ExchangeClient client = referenceClientMap.remove(key);
            if (client != null) {
                try {
                    if (logger.isInfoEnabled()) {
                        logger.info("Close dubbo connect: " + client.getLocalAddress() + "-->" + client.getRemoteAddress());
                    }
                    client.close(ConfigurationUtils.getServerShutdownTimeout());
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            }
        }

        for (String key : new ArrayList<String>(ghostClientMap.keySet())) {
            ExchangeClient client = ghostClientMap.remove(key);
            if (client != null) {
                try {
                    if (logger.isInfoEnabled()) {
                        logger.info("Close dubbo connect: " + client.getLocalAddress() + "-->" + client.getRemoteAddress());
                    }
                    client.close(ConfigurationUtils.getServerShutdownTimeout());
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            }
        }
        stubServiceMethodsMap.clear();
        super.destroy();
    }
}
