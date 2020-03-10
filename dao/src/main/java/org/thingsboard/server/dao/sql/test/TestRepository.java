package org.thingsboard.server.dao.sql.test;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.Student;
import org.thingsboard.server.dao.model.sql.StudentEntity;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;

/**
 * @author hanjinqun
 * @date 2020/3/4
 */
@SqlDao
public interface TestRepository extends JpaRepository<StudentEntity, String> {

    /**
     * 根据性别姓名动态查询学生
     * */
    @Query(value = "SELECT s FROM StudentEntity s where 1=1 " +
            "and (?1 is null or ?1='' or s.sex=?1) " +
            "and (?2 is null or ?2='' or LOWER(s.name) LIKE LOWER(CONCAT('%',?2, '%')))")
    List<StudentEntity> findStudent(@Param("sex") String sex, @Param("name") String name);

    /**
     * 根据班级名称查询学生信息列表
     * */
    @Query(value = "select b.id as bId,s.id as sId,b.name as bName,s.name as sName,s.sex as sex,s.grade from banji b " +
            "left join ban_stu bs on bs.bId=b.id " +
            "left join student s on s.id=bs.sId where 1=1 " +
            "and (?1 is null or ?1='' or b.name=?1)",nativeQuery = true)
    List<Object[]> findBSByMiX(@Param("bName")String bName);
    /**
     * 查询学生数量
     * */
    @Query(value = "select count(s.id) from StudentEntity s")
    int countStu();
    /**
     * 分页查询学生表
     * */
    @Query(value = "select s from StudentEntity s where sex=?1")
    Page<StudentEntity>stuPage1(String sex, Pageable pageable);

}
