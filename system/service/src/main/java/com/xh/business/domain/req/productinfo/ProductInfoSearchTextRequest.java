package com.xh.business.domain.req.productinfo;

import com.xh.common.core.web.PageQuery;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: QiMu
 * @Date: 2023/09/04 11:33:48
 * @Version: 1.0
 * @Description: 产品信息搜索文本请求
 */
@Data
public class ProductInfoSearchTextRequest implements Serializable {

    /**
     * 搜索文本
     */
    private String searchText;
}
