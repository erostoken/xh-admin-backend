package com.xh.common.core.web;

/**
 * 2021-09-26 sunxh
 * 业务异常，用于service抛出业务错误信息
 */
public class MyException extends RuntimeException {

    private final int code;

    public MyException(int code, String message) {
        super(message);
        this.code = code;
    }

    public MyException(String message) {
        this(500, message);
    }

    public MyException(int code, Throwable e) {
        super(e);
        this.code = code;
    }

    public MyException(Throwable e) {
        this(500, e);
    }

    public int getCode() {
        return code;
    }

}
