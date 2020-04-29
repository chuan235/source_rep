package top.gmfcj.proxy;



public class IndexCotroller /*extends HttpServlet*/{


    public String doGet(){
        AsyncFactory.syncCallLog("hello", "/hello", "访问首页");
        return "hello.html";
    }



}
