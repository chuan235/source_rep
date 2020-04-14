package top.gmfcj.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.gmfcj.bean.BookInfo;
import top.gmfcj.mapper.BookInfoMapper;

import java.util.List;


@Service
public class BookInfoService {

	@Autowired
	BookInfoMapper bookInfoMapper;

	@Transactional
	public List<BookInfo> selectAll() {
		return bookInfoMapper.selectAllBook();
	}

}
