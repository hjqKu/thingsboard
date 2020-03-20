package com.loit.dao.model.sql;

import com.loit.dao.util.mapping.JsonStringType;
import lombok.Data;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

/**
 * @author hanjinqun
 * @date 2020/3/18
 */
@Data
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = "iot_gateway_info")
public class GatewayInfoEntity {
    @Id
    @Column(name = "id")
    private String id;
    @Column(name = "device_id")
    private String DeviceId;
    @Column(name = "tenant_id")
    private String tenantId;
    @Column(name = "user_id")
    private String userId;
    @Column(name = "gateway_name")
    private String gatewayName;
    @Column(name = "gateway_ip")
    private String gatewayIp;
    @Column(name = "gateway_port")
    private String gatewayPort;
    @Column(name = "use_company")
    private String useCompany;
    @Column(name = "remark")
    private String remark;
    @Column(name = "create_time")
    private Date createTime;
    @Column(name = "update_time")
    private Date updateTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDeviceId() {
        return DeviceId;
    }

    public void setDeviceId(String deviceId) {
        DeviceId = deviceId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getGatewayName() {
        return gatewayName;
    }

    public void setGatewayName(String gatewayName) {
        this.gatewayName = gatewayName;
    }

    public String getGatewayIp() {
        return gatewayIp;
    }

    public void setGatewayIp(String gatewayIp) {
        this.gatewayIp = gatewayIp;
    }

    public String getGatewayPort() {
        return gatewayPort;
    }

    public void setGatewayPort(String gatewayPort) {
        this.gatewayPort = gatewayPort;
    }

    public String getUseCompany() {
        return useCompany;
    }

    public void setUseCompany(String useCompany) {
        this.useCompany = useCompany;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
