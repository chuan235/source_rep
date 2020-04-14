package top.gmfcj.mapper;


import org.apache.ibatis.annotations.*;
import top.gmfcj.bean.BookInfo;

import java.util.List;

@Mapper
public interface BookInfoMapper {


	@Results(id = "bookResult", value = {
			@Result(property = "bookId", column = "book_id", id = true),
			@Result(property = "classId", column = "class_id")
	})
	@Select("select * from book_info")
	public List<BookInfo> selectAllBook();

	@Results(id = "bookResult2", value = {
			@Result(property = "bookId", column = "book_id", id = true),
			@Result(property = "classId", column = "class_id")
	})
	@Select("select * from book_info where book_id = #{bookId}")
	public BookInfo selectByBookId(@Param("book_id") Long bookId);



}
