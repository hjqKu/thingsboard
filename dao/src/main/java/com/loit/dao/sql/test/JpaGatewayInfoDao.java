package com.loit.dao.sql.test;

import com.loit.dao.model.sql.GatewayInfoEntity;
import com.loit.dao.sql.jpaAbstractSearchIotDao;
import com.loit.dao.test.GatewayInfoDao;
import com.loit.dao.util.SqlDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;


/**
 * @author hanjinqun
 * @date 2020/3/19
 */
@Component
@SqlDao
public class JpaGatewayInfoDao extends jpaAbstractSearchIotDao<GatewayInfoEntity> implements GatewayInfoDao{
    @Autowired
    private GatewayInfoRepository gatewayInfoRepository;
    @Override
    protected CrudRepository getCrudRepository() {
        return gatewayInfoRepository;
    }
    @Override
    public GatewayInfoEntity findaa() {
        return null;
    }

}
