package com.xh.business.domain.req.interfaceinfo;

import com.xh.common.core.web.PageQuery;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: QiMu
 * @Date: 2023/09/04 11:33:35
 * @Version: 1.0
 * @Description: 界面信息搜索文本请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InterfaceInfoSearchTextRequest extends PageQuery implements Serializable {
    private static final long serialVersionUID = -6337349622479990038L;

    private String searchText;
}
