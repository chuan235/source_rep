package top.gmfcj;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Test;
import top.gmfcj.bean.Admin;
import top.gmfcj.mapper.AdminMapper;
import top.gmfcj.test.InvokTest;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

/**
 * @description:
 * @author: GMFCJ
 * @create: 2019-09-13 08:59
 */
public class StartMyBatis {


    public static void main(String[] args) throws IOException {
        InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
        final SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        SqlSession session1 = sqlSessionFactory.openSession(true);
//        System.out.println(session1.getMapper(AdminMapper.class).selectAdminById(2));
//        System.out.println(session1.getMapper(AdminMapper.class).selectAdminById(2));
        AdminMapper mapper = session1.getMapper(AdminMapper.class);
        Admin admin = mapper.selectAdminById(2);
        admin.setPassword("124241dasFafas");
        System.out.println(mapper.updatePassword(admin));
        session1.close();
//        for (int i = 0; i < 10; i++) {
//            new Thread(() -> {
//                SqlSession session = sqlSessionFactory.openSession();
//                AdminMapper mapper = session.getMapper(AdminMapper.class);
//                System.out.println(mapper.selectAdminById(2));
//                session.commit();
//                session.close();
//            }).start();
//        }

//            SqlSession session = sqlSessionFactory.openSession();
//            AdminMapper mapper = session.getMapper(AdminMapper.class);
//            Admin admin = mapper.selectAdminById(2);
//            admin.setPassword("password123test");
//            mapper.updatePassword(admin);
//            mapper.selectAdminById(2);

//            System.out.println(mapper.queryAll());
//            System.out.println(mapper.selectAdminById(1));
//            session.close();


    }


    @Test
    public void test(){
        Class<InvokTest> testClass = InvokTest.class;

        try {
            Method method = testClass.getMethod("toString", null);
            // top.gmfcj.test.InvokTest 获取方法所在的class
            System.out.println(method.getDeclaringClass().getName());
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

    }
}
