package top.gmfcj.cmd;

import org.junit.Test;

import java.io.*;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

public class JavaCommandTest {


    @Test
    public void test() {
        //testProcessBuilder();
        //testRunTime();
//        List<String> command = new ArrayList<>();
//        command.add("xxx/a.exe");
//        command.add("-i");
//        command.add("-y");
//        testRunTime();
        System.out.println(System.getProperty("java.io.tmpdir"));
//        testProcessBuilder();
        LocalDate firstDayOfMonth = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());

//        System.out.println(firstDayOfMonth);
//
        Properties properties = System.getProperties();
        Enumeration<?> names = properties.propertyNames();
        while (names.hasMoreElements()) {
            Object key = names.nextElement();
            Object value = properties.get(key);
            System.out.println("" + key + "     ,     " + value);
        }
    }

    public void testProcessBuilder() throws IOException {
//        System.getenv()
        ProcessBuilder processBuilder = new ProcessBuilder();
//        processBuilder.environment()
        //processBuilder.command("ping 127.0.0.1");
//        processBuilder.command("ipconfig");
//        processBuilder.command("jps");
        List<String> commands = new ArrayList<>();
        commands.add("D:/env/java/jdk8.171/bin/java.exe");
        commands.add("-version");
        processBuilder.command(commands);
        // 指定程序的工作目录
        File file = new File(System.getProperty("java.io.tmpdir") + "/process");
        if(!file.exists()){
            file.createNewFile();
        }
        processBuilder.directory(file);
        //将标准输入流和错误输入流合并，通过标准输入流读取信息         
        processBuilder.redirectErrorStream(true);
        try {
            // 启动进程
            Process process = processBuilder.start();
            // 获取流
            InputStream inputStream = process.getInputStream();
            InputStreamReader reader = new InputStreamReader(inputStream, Charset.forName("gbk"));
            // 读取
            int len;
            char[] buffer = new char[1024];
            // rs
            StringBuffer result = new StringBuffer();
            while ((len = reader.read(buffer)) != -1) {
                String b = new String(buffer, 0, len);
                result.append(b);
                System.out.println(b);
            }
            inputStream.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void testRunTime() {
        try {
            // Process process = Runtime.getRuntime().exec("cmd.exe /C netstat -ano");
            String[] command = new String[]{
                    "cmd", "/c", "netstat -an | findstr :80"
            };
            Runtime runtime = Runtime.getRuntime();
//            Process process = Runtime.getRuntime().exec("cmd.exe /k netstat -an | findstr :80");
//            Process process = Runtime.getRuntime().exec(command);
//            Process process = runtime.exec("wmic process where caption=\"javac.exe\" get caption,commandline /value");
//            Process process = runtime.exec("cmd.exe /k java -version");  // 执行不了
//            Process process = runtime.exec("cmd /k d: && cd d:/env/apache-tomcat-9.0.21/bin && startup.bat");
//            Process process = runtime.exec("cmd /C d: && d:/env/java/jdk8.171/bin/java.exe -version");
            Process process = runtime.exec("cmd /C java -version");
//            Process process = runtime.exec("cmd /C ant -version");
//            Process process = runtime.exec("cmd /C mvn -v");
//            Process process = runtime.exec("cmd /C node -v");
            // 获取流
            InputStream inputStream = process.getInputStream();
            InputStreamReader reader = new InputStreamReader(inputStream, Charset.forName("gbk"));
            // 读取
            int len;
            char[] buffer = new char[1024];
            // rs
            StringBuffer result = new StringBuffer();
            while ((len = reader.read(buffer)) != -1) {
                String b = new String(buffer, 0, len);
                result.append(b);
                System.out.println(b);
            }
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void testExit() throws IOException {
        Process pro = Runtime.getRuntime().exec("cmd.exe /C netstat -an | findstr :80");
        int status = pro.exitValue();
        System.out.println("退出的值：" + status);

    }

    @Test
    public void testExit2() throws IOException, InterruptedException {
        Process pro = Runtime.getRuntime().exec("cmd.exe /C netstat -an | findstr :80");
        int status = pro.waitFor();
        System.out.println("退出的值：" + status);
        Runtime.getRuntime().exit(0);

    }
}
