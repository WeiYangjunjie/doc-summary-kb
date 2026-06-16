package com.example.dockb.client;

/**
 * M3 调用失败统一异常。
 */
public class M3Exception extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public M3Exception(String message) {
        super(message);
    }

    public M3Exception(String message, Throwable cause) {
        super(message, cause);
    }
}