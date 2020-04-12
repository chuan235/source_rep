package top.gmfcj.servlet;


import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class MyServlet extends HttpServlet {

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.out.println("请求来了，由service进行分发");
		doGet(request, response);
//		doPost(request, response);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.out.println("myServlet doGet ");
	}

	@Override
	public void init() throws ServletException {
		System.out.println("初始化后就执行init");
		super.init();
	}
}
