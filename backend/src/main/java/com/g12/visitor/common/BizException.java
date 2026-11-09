package com.g12.visitor.common;

import lombok.Getter;

/**
 * 业务异常。httpStatus 决定 HTTP 状态码与 Result.code，
 * bizCode 放进 Result.message（固定业务码字符串）。
 */
@Getter
public class BizException extends RuntimeException {

    private final int httpStatus;
    private final String bizCode;

    public BizException(int httpStatus, String bizCode) {
        super(bizCode);
        this.httpStatus = httpStatus;
        this.bizCode = bizCode;
    }

    public BizException(int httpStatus, String bizCode, String detail) {
        super(detail);
        this.httpStatus = httpStatus;
        this.bizCode = bizCode;
    }

    public static BizException badRequest(String bizCode) {
        return new BizException(400, bizCode);
    }

    public static BizException unauthorized(String bizCode) {
        return new BizException(401, bizCode);
    }

    public static BizException forbidden(String bizCode) {
        return new BizException(403, bizCode);
    }

    public static BizException notFound() {
        return new BizException(404, ErrorCode.NOT_FOUND);
    }

    public static BizException conflict(String bizCode) {
        return new BizException(409, bizCode);
    }
}
