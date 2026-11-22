package org.chengyuan.cyproject.error.errors;

import org.chengyuan.cyproject.error.ErrorCode;

public class BizException extends RuntimeException {
    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}