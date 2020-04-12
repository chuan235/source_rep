package top.gmfcj.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

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

}
