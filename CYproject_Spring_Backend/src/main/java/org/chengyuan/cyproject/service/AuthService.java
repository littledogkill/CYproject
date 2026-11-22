package org.chengyuan.cyproject.service;

import org.chengyuan.cyproject.dto.request.LoginRequest;
import org.chengyuan.cyproject.dto.request.RegisterRequest;
import org.chengyuan.cyproject.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse login(LoginRequest request);

    AuthResponse register(RegisterRequest request);
}
