package top.gmfcj.bean;

import java.io.Serializable;

/**
 * @description:
 * @author: GMFCJ
 * @create: 2019-09-13 09:00
 */
public class Admin implements Serializable {
    private Integer adminId;
    private String password;
    private String userName;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Admin() {
    }

    public Admin(Integer adminId, String password) {
        this.adminId = adminId;
        this.password = password;
    }

    public Integer getAdminId() {
        return adminId;
    }

    public void setAdminId(Integer adminId) {
        this.adminId = adminId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "Admin{" +
                "adminId=" + adminId +
                ", password='" + password + '\'' +
                '}';
    }
}
