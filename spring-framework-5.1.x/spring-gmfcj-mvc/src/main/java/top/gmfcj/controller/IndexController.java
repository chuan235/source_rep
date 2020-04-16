package top.gmfcj.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

@Controller
public class IndexController {

	@RequestMapping("/index")
	@ResponseBody
	public String index(){
		System.out.println("index controller");
		return "index";
	}

	@RequestMapping("/json")
	@ResponseBody
	public Map<String,String> json(){
		Map<String,String> map = new HashMap<>();
		map.put("data", "user{xxx}");
		map.put("code", "200");
		map.put("msg", "success");
		return map;
	}

	@RequestMapping("/param")
	@ResponseBody
	public String param(String param1, String param2){
		System.out.println("param1="+param1+", param2="+param2);
		System.out.println("index controller");
		return "index";
	}


//	@Override
//	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
//		return null;
//	}
}
