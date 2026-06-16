package com.example.dockb.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Result / ResultCode 单元测试。
 */
class ResultTest {

    @Test
    void success_returnsCode0() {
        Result<String> r = Result.success("hello");
        assertEquals(0, r.getCode());
        assertEquals("hello", r.getData());
        assertNull(r.getMessage());
    }

    @Test
    void successWithNullData() {
        Result<Void> r = Result.success(null);
        assertEquals(0, r.getCode());
        assertNull(r.getData());
    }

    @Test
    void error_returnsCodeAndMessage() {
        Result<?> r = Result.error(500, "内部错误");
        assertEquals(500, r.getCode());
        assertEquals("内部错误", r.getMessage());
        assertNull(r.getData());
    }

    @Test
    void error_fromResultCode() {
        Result<?> r = Result.error(ResultCode.PARAM_INVALID);
        assertEquals(ResultCode.PARAM_INVALID.getCode(), r.getCode());
        assertEquals(ResultCode.PARAM_INVALID.getMessage(), r.getMessage());
    }

    @Test
    void resultCode_allHaveUniqueCodes() {
        var codes = ResultCode.values();
        long distinct = java.util.Arrays.stream(codes).map(ResultCode::getCode).distinct().count();
        assertEquals(codes.length, distinct, "All ResultCode values must have unique codes");
    }

    @Test
    void resultCode_noneHasNullMessage() {
        for (ResultCode rc : ResultCode.values()) {
            assertNotNull(rc.getMessage(), "ResultCode " + rc.name() + " has null message");
            assertFalse(rc.getMessage().isBlank(), "ResultCode " + rc.name() + " has blank message");
        }
    }
}
