package top.gmfcj.component;

import top.gmfcj.service.IUserService;

//@PropertySource(value = "application.propertites")
//@Component
public class UserTest2 {


//	@Autowired
//	@Resource
//	//@Inject
//	UserTest3 userTest3;

	IUserService userServiceImpl1;


//	@Autowired
//	public void setUserServiceImpl1(IUserService userServiceImpl1){
//		this.userServiceImpl1 = userServiceImpl1;
//	}
//
//	@PreDestroy
//	@PostConstruct
	public void test(){
		System.out.println("test");
	}


	/**
	 * 这样写会出现错误，应为@Resource是根据这个属性名称去找bean对象的，这里就出现了找到的bean和这里的类型不一致出现错误
	 * 但是如果容器中不存在bean名称为 userTest3 的对象，那么就会根据类型去装配,这样就可以装配成功了
	 * 说明 @Resource 默认优先使用ByName的装配技术，byName找不到的时候，进行byType装配
	 */
//	@Resource
//	UserTest1 userTest3;

//	@Resource
//	UserTest3 userTest3;
//	@Resource
//	UserTest1 userTest1;


//	@Autowired
//	UserTest3 userTest3;


//	@Autowired
//	UserTest1 userTest1;
//
//	public UserTest3 getUserTest3() {
//		return userTest3;
//	}
//	@Autowired
//	@Resource
//	public void setUserTest3(UserTest3 userTest3) {
//		this.userTest3 = userTest3;
//	}
//
//	public UserTest1 getUserTest1() {
//		return userTest1;
//	}
////	@Autowired
////	@Resource
//	public void setUserTest1(UserTest1 userTest1) {
//		this.userTest1 = userTest1;
//	}

//	private String name;
//
//	public String getName() {
//		return name;
//	}
//
//	public void setName(String name) {
//		this.name = name;
//	}
}
