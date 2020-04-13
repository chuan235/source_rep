package top.gmfcj.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

import java.util.HashMap;
import java.util.Map;

@Controller
public class IndexController {

    /**
     * http://localhost:8080/param?name=123&school=123
     */
    @RequestMapping("/param")
    @ResponseBody
    public Map<String, String> param(@RequestParam("name") String name, @RequestParam("school") String school){
        System.out.println("param controller");
        Map<String,String> map = new HashMap<>();
        map.put("name",name);
        map.put("school",school);
        map.put("controller", "param controller");
        return map;
    }

    @RequestMapping("/index")
    @ResponseBody
    public Map<String, String> index(){
        System.out.println("index controller");
        Map<String,String> map = new HashMap<>();
        map.put("index","index Controller");
        return map;
    }

    @RequestMapping("/page")
    public ModelAndView helloPage(){
        ModelAndView view = new ModelAndView();
        // 设置了view 底层就是转发到一个
        view.setViewName("page.html");
        return view;
    }


    @GetMapping("/fpage")
    public String forward(){
//        return "page.html";
        return "page";
    }
}
