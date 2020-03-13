package com.loit.common.data;

import java.io.Serializable;

/**
 * @author hanjinqun
 * @date 2020/3/5
 * 班级学生表联查实体
 */
public class BanStu implements Serializable {
    private String bId;
    private String sId;
    private String bName;
    private String sName;
    private String sex;
    private String grade;
    public BanStu(){

    }

    public String getbId() {
        return bId;
    }

    public void setbId(String bId) {
        this.bId = bId;
    }

    public String getsId() {
        return sId;
    }

    public void setsId(String sId) {
        this.sId = sId;
    }

    public String getbName() {
        return bName;
    }

    public void setbName(String bName) {
        this.bName = bName;
    }

    public String getsName() {
        return sName;
    }

    public void setsName(String sName) {
        this.sName = sName;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public BanStu(String bId, String sId, String bName, String sName, String sex, String grade) {
        this.bId = bId;
        this.sId = sId;
        this.bName = bName;
        this.sName = sName;
        this.sex = sex;
        this.grade = grade;
    }
}
