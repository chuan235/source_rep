package top.gmfcj.client.zkclient;

import org.I0Itec.zkclient.exception.ZkMarshallingError;
import org.I0Itec.zkclient.serialize.ZkSerializer;

import java.nio.charset.Charset;

public class MyZkSerializer implements ZkSerializer {

    /**
     * 序列化，将对象转化为字节数组
     */
    @Override
    public byte[] serialize(Object obj) throws ZkMarshallingError {
        return String.valueOf(obj).getBytes(Charset.forName("UTF-8"));
    }

    /**
     * 反序列化，将字节数组转化为UTF_8字符串
     */
    @Override
    public Object deserialize(byte[] bytes) throws ZkMarshallingError {
        return new String(bytes, Charset.forName("UTF-8"));
    }
}
