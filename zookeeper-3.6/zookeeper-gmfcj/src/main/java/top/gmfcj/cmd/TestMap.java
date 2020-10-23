package top.gmfcj.cmd;


import java.util.ArrayList;
import java.util.List;

public class TestMap {

    public static void main(String[] args) {
        TestMap test = new TestMap();
        List<String> list = new ArrayList<>();
        list.add("abc");
        list.add("abc2");
        list.add("abc3");
        test.test(list);
        System.out.println(list);
    }

    public void test(List<String> list){
        list.clear();
        list.add("3213");
    }

}
