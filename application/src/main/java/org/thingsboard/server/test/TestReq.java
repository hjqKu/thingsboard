package org.thingsboard.server.test;

import java.io.Serializable;

/**
 * @author hanjinqun
 * @date 2020/3/4
 */
public class TestReq implements Serializable {
    private String sex;
    private String sname;
    private String bName;

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getSname() {
        return sname;
    }

    public void setSname(String sname) {
        this.sname = sname;
    }

    public String getbName() {
        return bName;
    }

    public void setbName(String bName) {
        this.bName = bName;
    }
}
