package org.chengyuan.cyproject.util;

/**
 * 保存当前请求认证用户的 id。
 * 由 JwtFilter 在请求进入时写入、请求结束时清理；
 * Service 层通过 {@link #getUserId()} 获取当前登录用户，避免在方法间透传 token。
 */
public final class UserContext {

    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();

    private UserContext() {
    }

    public static void setUserId(String userid) {
        USER_ID.set(userid);
    }

    public static String getUserId() {
        return USER_ID.get();
    }

    public static void clear() {
        USER_ID.remove();
    }
}
