
package org.thingsboard.server.dao.sql.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.StudentEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextTestDao;
import org.thingsboard.server.dao.test.TestDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.ArrayList;
import java.util.List;

/**
 * @author hanjinqun
 * @date 2020/3/4
 */
@Component
@SqlDao
public class JpaTestDao extends JpaAbstractSearchTextTestDao<StudentEntity, Student> implements TestDao {
    @Autowired
    private TestRepository testRepository;

    @Override
    protected Class<StudentEntity> getEntityClass() {
        return StudentEntity.class;
    }

    @Override
    protected CrudRepository<StudentEntity, String> getCrudRepository() {
        return testRepository;
    }



    @Override
    public List<Student> findStudent(String sex,String name) {
        List<StudentEntity>entities=testRepository.findStudent(sex, name);
        return DaoUtil.convertDataList(entities);
    }


    @Override
    public List<BanStu> findBSByMiX(String bName) {
        List<BanStu>result=new ArrayList<>();
        try {
            List<Object[]>objects=testRepository.findBSByMiX(bName);
            if(objects.size()>0){
                result=ObjectConvertUtils.objectToBean(objects,BanStu.class);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public int countStu() {
        return testRepository.countStu();
    }

    @Override
    public Page<StudentEntity> stuPage1(String sex, Pageable pageable) {
        return testRepository.stuPage1(sex,pageable);
    }
}
