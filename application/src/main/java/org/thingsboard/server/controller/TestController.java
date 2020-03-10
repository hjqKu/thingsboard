package org.thingsboard.server.controller;

import com.datastax.driver.core.utils.UUIDs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.BanStu;
import org.thingsboard.server.common.data.Student;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.dao.model.sql.StudentEntity;
import org.thingsboard.server.dao.sql.test.TestRepository;
import org.thingsboard.server.dao.test.TestDao;
import org.thingsboard.server.test.PageUtil;
import org.thingsboard.server.test.TestPageReq;
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
    @Autowired
    private TestRepository testRepository;
    public static void main(String[] args) {
//        System.out.println(UUID.randomUUID());
//        System.out.println(UUID.randomUUID().toString());
//        TenantId aa=new TenantId(UUID.randomUUID());
        System.out.println(UUIDs.timeBased().toString());
        System.out.println(UUID.fromString(UUIDs.timeBased().toString()));
        TenantId tenantId=new TenantId(UUID.fromString("ad513ae0-62a1-11ea-8027-8b4eb899b278"));

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
    /**
     * 查询学生分页简单
     * */
    @PostMapping(value = "/test/stuPage1")
    public Page<StudentEntity>  stuPage1(@RequestBody TestPageReq req){
        //1.适用无任何查询条件 可以直接调用
//      Page<StudentEntity> entities=testRepository.findAll(new PageRequest(req.getPage(),req.getSize()));
        //适用单表
        Pageable pageable = PageRequest.of(req.getPage(),req.getSize());
        return testDao.stuPage1(req.getSex(),pageable);
    }
    /**
     * 查询学生代码分页
     * */
    @PostMapping(value = "/test/stuPage2")
    public PageUtil stuPage2(@RequestBody TestPageReq req){
        List<BanStu> banStuList=testDao.findBSByMiX(req.getbName());
        PageUtil<BanStu> pageUtil=new PageUtil<BanStu>(req.getPage(),req.getSize(),banStuList);
        return pageUtil;
    }

}
