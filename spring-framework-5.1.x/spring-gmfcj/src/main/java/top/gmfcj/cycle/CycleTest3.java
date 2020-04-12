package top.gmfcj.cycle;


import org.springframework.stereotype.Component;
import top.gmfcj.anno.AopAnno;

@Component
public class CycleTest3 {

//	@Autowired
//	private CycleTest1 cycleTest1;
	public CycleTest3(){
		//System.out.println("creat CycleTest3");
	}


	@AopAnno
	public void hello(){
		System.out.println("cycle test3 hello");
	}


}
