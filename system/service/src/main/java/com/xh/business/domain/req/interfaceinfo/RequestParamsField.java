package com.xh.business.domain.req.interfaceinfo;

import java.util.List;
import lombok.Data;

/**
 * @Author: QiMu
 * @Date: 2023/09/15 03:52:36
 * @Version: 1.0
 * @Description: 请求参数字段
 */
@Data
public class RequestParamsField {
    /**
     * 字段ID
     */
    private String id;
    /**
     * 字段名称
     */
    private String fieldName;
    /**
     * 字段类型
     */
    private String type;
    /**
     * 字段描述
     */
    private String desc;
    /**
     * 是否必填
     */
    private String required;
    /**
     * 示例
     */
    private String example;
    /**
     * 子集 - 只有type为object或array时才有值
     */
    private List<RequestParamsField> children;
}