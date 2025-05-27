package com.xh.common.core.web;

import java.io.Serializable;
import lombok.Data;

/**
 * 删除请求
 *
 * @author qimu
 */
@Data
public class DeleteRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    private Long id;
}