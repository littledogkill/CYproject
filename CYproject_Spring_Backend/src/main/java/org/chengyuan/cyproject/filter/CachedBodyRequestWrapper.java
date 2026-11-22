package org.chengyuan.cyproject.filter;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    public CachedBodyRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        // 一次性把原始流读完，缓存到内存
        this.cachedBody = request.getInputStream().readAllBytes();
    }

    /** 拿到原始 body 字符串 */
    public String getBodyAsString() {
        return new String(cachedBody, StandardCharsets.UTF_8);
    }

    /** 关键：每次调用都返回一个新的、基于缓存的流，保证可重复读 */
    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream bais = new ByteArrayInputStream(cachedBody);
        return new ServletInputStream() {
            @Override public int read() { return bais.read(); }
            @Override public int read(byte[] b, int off, int len) { return bais.read(b, off, len); }
            @Override public boolean isFinished() { return bais.available() == 0; }
            @Override public boolean isReady() { return true; }
            @Override public void setReadListener(ReadListener listener) { /* no-op */ }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(cachedBody), StandardCharsets.UTF_8));
    }
}