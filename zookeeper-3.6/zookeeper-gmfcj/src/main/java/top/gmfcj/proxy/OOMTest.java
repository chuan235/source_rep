package top.gmfcj.proxy;

import top.gmfcj.jvmm.ConcurrentHashMap2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * -Xmx2m -XX:+HeapDumpOnOutOfMemoryError
 * @description: oom生成dump文件
 */
public class OOMTest {

    // 必须由bootstrap 启动类加载器加载  才可以使用这个unsafe
    private static final sun.misc.Unsafe UNSAFE = sun.misc.Unsafe.getUnsafe();

    private static final long PROBE = 0L;

    static {
        try {
            Class tk = Thread.class;
            UNSAFE.objectFieldOffset
                    (tk.getDeclaredField("threadLocalRandomProbe"));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
//        List<String> list = new ArrayList<>();
//        for (int i = 1; i > 0; i++) {
//            String s = "list" + i + "_string";
//            list.add(s);
//        }
        for (int i = 1; i < 10; i++) {
            System.out.println(tableSizeFor(i));
            System.out.println(UNSAFE.getInt(Thread.currentThread(), PROBE));
        }

    }
    private static final int tableSizeFor(int c) {
        int n = c - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return n;
    }

}
