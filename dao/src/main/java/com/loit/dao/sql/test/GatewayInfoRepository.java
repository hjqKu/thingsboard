package com.loit.dao.sql.test;

import com.loit.dao.model.sql.GatewayInfoEntity;
import com.loit.dao.util.SqlDao;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author hanjinqun
 * @date 2020/3/19
 */
@SqlDao
public interface GatewayInfoRepository  extends JpaRepository<GatewayInfoEntity,String> {

}
