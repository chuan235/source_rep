package top.gmfcj.cycle;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.gmfcj.anno.AopAnno;

@Component
public class CycleTest1 {

	@Autowired
	private CycleTest2 cycleTest2;

	public CycleTest1(){
		//System.out.println("creat cycleTest1");
	}

	@AopAnno
	public void hello(){
		System.out.println("cycle test1 hello");
	}

}
