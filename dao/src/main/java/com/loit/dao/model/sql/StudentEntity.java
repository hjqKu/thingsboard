package com.loit.dao.model.sql;

import com.loit.common.data.Student;
import com.loit.common.data.id.StudentId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.TypeDef;
import com.loit.dao.model.BaseSqlEntity;
import com.loit.dao.model.SearchTextEntity;
import com.loit.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author hanjinqun
 * @date 2020/3/4
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = "student")
public class StudentEntity extends BaseSqlEntity<Student>implements SearchTextEntity<Student> {
    @Column(name = "name")
    private String name;
    @Column(name = "sex")
    private String sex;
    @Column(name = "grade")
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

    public StudentEntity() {
        super();
    }

    public StudentEntity(Student student) {
        if (student.getId() != null) {
            System.out.println(student.getId());
            System.out.println(student.getId().getId());
            this.setId(student.getId().getId());
        }
        this.name = student.getName();
        this.sex = student.getSex();
        this.grade = student.getGrade();
    }

    @Override
    public String getSearchTextSource() {
        return null;
    }

    @Override
    public void setSearchText(String searchText) {

    }

    @Override
    public Student toData() {
        Student student=new Student(new StudentId(getId()));
        student.setGrade(grade);
        student.setName(name);
        student.setSex(sex);
        return student;
    }
}
