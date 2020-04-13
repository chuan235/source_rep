package top.gmfcj.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

//@Mapper
public interface AdminMapper {

    @Select("select * from admin")
    public List<Map<String,Object>> query();

}
