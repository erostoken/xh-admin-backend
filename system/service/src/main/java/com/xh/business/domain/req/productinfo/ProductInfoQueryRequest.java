package com.xh.business.domain.req.productinfo;

import com.xh.common.core.web.PageQuery;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: QiMu
 * @Date: 2023/08/25 03:12:57
 * @Version: 1.0
 * @Description: 查询请求
 */
@Data
public class ProductInfoQueryRequest implements Serializable {

    /**
     * 产品名称
     */
    private String name;
    /**
     * 增加积分个数
     */
    private Integer addPoints;
    /**
     * 产品描述
     */
    private String description;

    /**
     * 商品状态（0- 默认下线 1- 上线）
     */
    private Integer status;

}