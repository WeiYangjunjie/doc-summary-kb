package com.example.dockb.common;

import lombok.Getter;

/**
 * 业务异常。所有可预期的业务错误抛出该异常，
 * 由 {@link GlobalExceptionHandler} 统一翻译为 {@link Result}。
 */
@Getter
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public BizException(ResultCode rc) {
        super(rc.getMessage());
        this.code = rc.getCode();
    }

    public BizException(ResultCode rc, String message) {
        super(message);
        this.code = rc.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}