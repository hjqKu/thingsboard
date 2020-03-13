
package com.loit.dao.test;

import com.loit.common.data.BanStu;
import com.loit.common.data.Student;
import com.loit.dao.TDao;
import com.loit.dao.model.sql.StudentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * @author hanjinqun
 * @date 2020/3/9
 */
public interface TestDao extends TDao<Student> {
    List<Student> findStudent(String sex,String name);
    List<BanStu>findBSByMiX(String bName);
    int countStu();
    Page<StudentEntity>stuPage1(String sex, Pageable pageable);
}
