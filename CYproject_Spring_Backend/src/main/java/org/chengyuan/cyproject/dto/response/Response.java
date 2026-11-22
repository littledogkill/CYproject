package org.chengyuan.cyproject.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.chengyuan.cyproject.error.ErrorCode;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Response<T> {

    private int code;

    private String message;

    private T data;

    public static <T> Response<T> ok(T data) {
        Response<T> r = new Response<>();
        r.code = ErrorCode.SUCCESS.getCode();
        r.message = ErrorCode.SUCCESS.getMessage();
        r.data = data;
        return r;
    }

    public static <T> Response<T> fail(ErrorCode ec) {
        return fail(ec.getCode(), ec.getMessage());
    }

    public static <T> Response<T> fail(int code, String message) {
        Response<T> r = new Response<>();
        r.code = code;
        r.message = message;
        r.data = null;
        return r;
    }
}
