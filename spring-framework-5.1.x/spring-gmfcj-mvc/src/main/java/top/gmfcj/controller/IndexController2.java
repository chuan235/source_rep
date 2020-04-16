package top.gmfcj.controller;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component("/index/2")
public class IndexController2 extends AbstractController {


	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
		System.out.println("IndexController1 --- AbstractController  class ");
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.addObject("hello index 2 page");
		return modelAndView;
	}
}
