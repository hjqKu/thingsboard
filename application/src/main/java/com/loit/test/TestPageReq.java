package com.loit.test;


import java.io.Serializable;

/**
 * @author hanjinqun
 * @date 2020/3/9
 */
public class TestPageReq implements Serializable {
    private Integer page;
    private Integer size;
    private String sex;
    private String bName;

    public String getbName() {
        return bName;
    }

    public void setbName(String bName) {
        this.bName = bName;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
