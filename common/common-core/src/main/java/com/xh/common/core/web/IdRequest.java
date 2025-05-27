package com.xh.common.core.web;

import java.io.Serializable;
import lombok.Data;

/**
 * @Author: QiMu
 * @Date: 2023/09/15 09:31:24
 * @Version: 1.0
 * @Description: id请求
 */
@Data
public class IdRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    private Long id;
}