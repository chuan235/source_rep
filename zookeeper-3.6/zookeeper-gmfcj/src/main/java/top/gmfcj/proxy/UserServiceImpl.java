package top.gmfcj.proxy;

import java.util.List;

@MyTransaction
public class UserServiceImpl implements IUserService {



    @Override
    public List<Object> findAll() {
        System.out.println("find all");
        return null;
    }
}
