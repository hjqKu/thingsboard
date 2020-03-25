package com.loit.controller;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loit.common.data.*;
import com.loit.common.data.audit.ActionType;
import com.loit.common.data.exception.ThingsboardException;
import com.loit.dao.model.sql.GatewayInfoEntity;
import com.loit.dao.test.GatewayInfoDao;
import com.loit.service.security.permission.Operation;
import com.loit.service.security.permission.Resource;
import com.loit.test.PageUtil;
import com.loit.test.TestPageReq;
import com.loit.test.TestReq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import com.loit.common.data.BanStu;
import com.loit.common.data.id.TenantId;
import com.loit.common.data.page.TextPageData;
import com.loit.dao.model.sql.StudentEntity;
import com.loit.dao.sql.test.TestRepository;
import com.loit.dao.test.TestDao;
import com.loit.test.PageUtil;
import com.loit.test.TestPageReq;
import com.loit.test.TestReq;

import java.util.List;
import java.util.UUID;

import static com.loit.common.data.UUIDConverter.fromTimeUUID;

/**
 * @author hanjinqun
 * @date 2020/3/4
 */
@RestController
@RequestMapping("/api")
public class TestController extends BaseController {
    @Autowired
    private TestRepository testRepository;
    @Autowired
    private TestDao testDao;
    @Autowired
    private GatewayInfoDao gatewayInfoDao;
    public static void main(String[] args)throws Exception {
        String tenantId="1ea5793ebb506e0968c59ca7e358b66";
        System.out.println(UUIDConverter.fromString("1ea5793ebb506e0968c59ca7e358b66"));
        ObjectMapper objectMapper=new ObjectMapper();
        String json="{\"gateway\":true}";
        JsonNode rootNode = objectMapper.readTree(json);
        System.out.println(rootNode);
    }

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

    //=========================新写法====================================
    /**
     *新增网关表
     * */
    @PostMapping(value = "/gateway/add")
    public GatewayInfoEntity gatwwayAdd(@RequestBody GatewayInfoEntity req)throws ThingsboardException {
        String tenantId="1ea5793ebb506e0968c59ca7e358b66";
        GatewayInfoEntity result=null;
        Device device=new Device();
        try {
            //新增网关信息
            req.setId(UUIDConverter.fromTimeUUID(UUIDs.timeBased()));
            req.setTenantId(tenantId);
            result=gatewayInfoDao.save(req);
            //新增设备基表

            device.setName(req.getGatewayName());
            device.setType("Basic");
            device.setTenantId(new TenantId(UUIDConverter.fromString(tenantId)));
            ObjectMapper objectMapper=new ObjectMapper();
            String json="{\"gateway\":true}";
            JsonNode rootNode = objectMapper.readTree(json);
            device.setAdditionalInfo(rootNode);

            Device savedDevice = checkNotNull(deviceService.saveDeviceWithAccessToken(device, null));

            actorService
                    .onDeviceNameOrTypeUpdate(
                            savedDevice.getTenantId(),
                            savedDevice.getId(),
                            savedDevice.getName(),
                            savedDevice.getType());

            logEntityAction(savedDevice.getId(), savedDevice,
                    savedDevice.getCustomerId(),
                    device.getId() == null ? ActionType.ADDED : ActionType.UPDATED, null);

            if (device.getId() == null) {
                deviceStateService.onDeviceAdded(savedDevice);
            } else {
                deviceStateService.onDeviceUpdated(savedDevice);
            }
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE), device,
                    null, device.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);
            throw handleException(e);
        }
        return result;
    }

}
