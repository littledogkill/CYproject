package org.chengyuan.cyproject.error;

import org.springframework.http.HttpStatus;

/**
 * 统一错误码。
 * code 字段用于前端解析（成功为 0，失败为非 0）；
 * httpStatus 用于 HTTP 响应状态码，两者独立维护。
 */
public enum ErrorCode {
    SUCCESS(0, "success", HttpStatus.OK),
    BAD_REQUEST(400, "请求参数无效", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(401, "未登录或 token 无效", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(401, "token 已过期", HttpStatus.UNAUTHORIZED),
    WRONG_CREDENTIALS(401, "邮箱或密码错误", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(403, "无权限执行该操作", HttpStatus.FORBIDDEN),
    NOT_FOUND(404, "资源不存在", HttpStatus.NOT_FOUND),
    EMAIL_EXISTS(409, "邮箱已被注册", HttpStatus.CONFLICT),
    USERNAME_EXISTS(409, "用户名已被使用", HttpStatus.CONFLICT),
    FILE_TOO_LARGE(413, "上传文件过大", HttpStatus.PAYLOAD_TOO_LARGE),
    NOT_IMPLEMENTED(501, "功能暂未实现", HttpStatus.NOT_IMPLEMENTED),
    INTERNAL_ERROR(500, "服务器内部错误", HttpStatus.INTERNAL_SERVER_ERROR),
    TOO_MANY_REQUESTS(429, "多次错误登录请求", HttpStatus.TOO_MANY_REQUESTS),;

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
