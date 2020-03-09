package org.thingsboard.server.controller;

import com.datastax.driver.core.utils.UUIDs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.BanStu;
import org.thingsboard.server.common.data.Student;
import org.thingsboard.server.dao.test.TestDao;
import org.thingsboard.server.test.TestReq;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.common.data.UUIDConverter.fromTimeUUID;

/**
 * @author hanjinqun
 * @date 2020/3/4
 */
@RestController
@RequestMapping("/api")
public class TestController {
    public static void main(String[] args) {
        System.out.println(UUIDs.timeBased());
        System.out.println(fromTimeUUID(UUID.fromString(UUIDs.timeBased().toString())));
    }
    @Autowired
    private TestDao testDao;
    /**
     * 新增修改学生
     * */
    @PostMapping(value = "/test/saveStudent")
    public Student saveStudent(@RequestBody Student student){
        return testDao.save(student);
    }
    /**
     * 测试查询学生
     * */
    @PostMapping(value = "/test/findStudent")
    public List<Student>findStudent(@RequestBody TestReq req){
        return testDao.findStudent(req.getSex(),req.getSname());
    }
    /**
     * 根据班级名称查询学生信息列表
     * */
    @PostMapping(value = "/test/findBSByMiX")
    public List<BanStu>findBSByMiX(@RequestBody TestReq req){
        return testDao.findBSByMiX(req.getbName());
    }
    /**
     * 查询学生数量
     * */
    @PostMapping(value = "/test/countStu")
    public int countStu(@RequestBody TestReq req){
        return testDao.countStu();
    }
}
