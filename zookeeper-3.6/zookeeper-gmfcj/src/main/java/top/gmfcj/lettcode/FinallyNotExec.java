package top.gmfcj.lettcode;

/**
 * @description: finally不会执行的四种情况
 */
public class FinallyNotExec {

    public static void main(String[] args) {
        try {
            // System.exit(0);
//            int i = 1/0;
//            System.exit(0);
            System.out.println("try commad  ....");
            Thread.sleep(1111);

            // 睡眠的时候关闭程序
        } catch (Exception e) {

        } finally {
//            int i = 1 / 0;
            System.out.println("finally main method");
        }


    }
}
