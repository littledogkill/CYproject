package org.chengyuan.cyproject.error;

import org.chengyuan.cyproject.dto.response.Response;
import org.chengyuan.cyproject.error.errors.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Response<Void>> handleBiz(BizException e) {
        ErrorCode ec = e.getErrorCode();
        return ResponseEntity.status(ec.getHttpStatus()).body(Response.fail(ec));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Response<Void>> handleUploadTooLarge(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(ErrorCode.FILE_TOO_LARGE.getHttpStatus())
                .body(Response.fail(ErrorCode.FILE_TOO_LARGE));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Response<Void>> handleUnreadableBody(HttpMessageNotReadableException e) {
        return ResponseEntity.status(ErrorCode.BAD_REQUEST.getHttpStatus())
                .body(Response.fail(ErrorCode.BAD_REQUEST));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response<Void>> handleOther(Exception e) {
        // 兜底异常必须记录日志，方便定位问题；对外只返回统一的 500
        log.error("未处理的异常", e);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getHttpStatus())
                .body(Response.fail(ErrorCode.INTERNAL_ERROR));
    }
}
