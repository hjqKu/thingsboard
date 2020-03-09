
package org.thingsboard.server.dao.test;

import org.thingsboard.server.common.data.BanStu;
import org.thingsboard.server.common.data.Student;
import org.thingsboard.server.dao.TDao;

import java.util.List;

/**
 * @author hanjinqun
 * @date 2020/3/9
 */
public interface TestDao extends TDao<Student> {
    List<Student> findStudent(String sex,String name);
    List<BanStu>findBSByMiX(String bName);
    int countStu();
}
