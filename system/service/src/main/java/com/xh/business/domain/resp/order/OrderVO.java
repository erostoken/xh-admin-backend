package com.xh.business.domain.resp.order;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * @Author: QiMu
 * @Date: 2023年09月19日 20:41
 * @Version: 1.0
 * @Description:
 */
@Data
public class OrderVO implements Serializable {
    private static final long serialVersionUID = -7340958009391771093L;
    private List<ProductOrderVO> records;
    private long total;
    private long size;
    private long current;
    private List<OrderItem> orders;
    private boolean optimizeCountSql;
    private boolean searchCount;
    private boolean optimizeJoinOfCountSql;
    private String countId;
    private Long maxLimit;
}
