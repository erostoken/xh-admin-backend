package com.xh.business.utils;

import com.xh.common.core.web.MyException;

/**
 * @Author: QiMu
 * @Date: 2023/09/15 09:31:43
 * @Version: 1.0
 * @Description: 自定义异常类
 */
public class BusinessException extends MyException {

    public BusinessException(int code, String message) {
        super(code, message);
    }

    public BusinessException(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMessage());
    }

    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode.getCode(), message);
    }

    public BusinessException(int code, Throwable e) {
        super(code, e);
    }

    public BusinessException(ErrorCode errorCode, Throwable e) {
        this(errorCode.getCode(), e);
    }

}
