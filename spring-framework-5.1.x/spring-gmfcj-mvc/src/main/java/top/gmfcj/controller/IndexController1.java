package top.gmfcj.controller;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component("/index/1")
public class IndexController1 implements Controller {

	@Override
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		System.out.println("IndexController1 --- Controller  interface ");
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.addObject("hello index 1 page");
		return modelAndView;
	}
}
