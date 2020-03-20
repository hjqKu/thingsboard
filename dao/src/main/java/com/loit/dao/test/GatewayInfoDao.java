package com.loit.dao.test;


import com.loit.dao.IotDao;
import com.loit.dao.model.sql.GatewayInfoEntity;

/**
 * @author hanjinqun
 * @date 2020/3/19
 */
public interface GatewayInfoDao extends IotDao<GatewayInfoEntity> {
    GatewayInfoEntity findaa();
}
