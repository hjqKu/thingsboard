package org.thingsboard.server.common.data.gateway;

import java.io.Serializable;

/**
 * @author hanjinqun
 * @date 2020/3/16
 * mqtt转换器
 */
public class Converter implements Serializable {
    //转换类型
    private String json;
    //过滤表达式
    private String filterExpression;
    //设备名称主题表达式
    private String deviceNameTopicExpression;
    //设备类型json表达式
    private String deviceTypeJsonExpression;
    //毫秒超时
    private Long timeout;

}
