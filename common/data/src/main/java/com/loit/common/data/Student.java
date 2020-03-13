package com.loit.common.data;

import com.loit.common.data.id.StudentId;
import lombok.EqualsAndHashCode;
import com.loit.common.data.id.StudentId;

import java.io.Serializable;

/**
 * @author hanjinqun
 * @date 2020/3/4
 */
@EqualsAndHashCode(callSuper = true)
public class Student  extends  SearchTextBasedWithAdditionalInfo<StudentId> implements Serializable {
    private String name;
    private String sex;
    private String grade;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    @Override
    public String getSearchText() {
        return name;
    }

    public Student(){
        super();
    }
    public Student(Student student){
        super(student);
        this.name=student.getName();
        this.sex=student.getSex();
        this.grade=student.getGrade();
    }
    public Student(StudentId id) {
        super(id);
    }

    @Override
    public String toString() {
        return "Student{" +
                "name='" + name + '\'' +
                ", sex='" + sex + '\'' +
                ", grade='" + grade + '\'' +
                ", createdTime=" + createdTime +
                ", id=" + id +
                '}';
    }
}
