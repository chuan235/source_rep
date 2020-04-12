package top.gmfcj.service.impl;

import org.springframework.stereotype.Service;
import top.gmfcj.service.Log;

@Service
public class DefaultLog implements Log {
	@Override
	public void write() {

		System.out.println("wirte log ........");
	}
}
