package top.gmfcj.cycle;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CycleTest2 {

	@Autowired
	private CycleTest1 cycleTest1;

	public CycleTest2(){
		//System.out.println("creat CycleTest2");
	}


}
