package top.gmfcj.mapper;

import org.apache.ibatis.annotations.Param;
import top.gmfcj.bean.Admin;

import java.util.List;

/**
 * @description:
 * @author: GMFCJ
 * @create: 2019-09-13 09:00
 */
public interface AdminMapper {

    public List<Admin> queryAll();

    public Admin selectAdminById(@Param("adminId") Integer adminId);

    public List<Admin> selectList(Admin admin);


    public int updatePassword(Admin admin);
}
