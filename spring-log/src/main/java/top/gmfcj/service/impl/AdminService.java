package top.gmfcj.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.gmfcj.mapper.AdminMapper;
import top.gmfcj.service.IAdminService;

import javax.annotation.Resource;

/**
 * @description:
 * @author: GMFCJ
 * @create: 2019-09-16 15:36
 */
@Service("admin")
public class AdminService /*implements IAdminService*/ {

    @Resource
    private AdminMapper adminMapper;

//    @Override
    public void show(){
        System.out.println("show");
    }
}
