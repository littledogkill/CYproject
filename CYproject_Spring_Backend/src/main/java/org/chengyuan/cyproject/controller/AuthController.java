package org.chengyuan.cyproject.controller;

import org.chengyuan.cyproject.dto.request.LoginRequest;
import org.chengyuan.cyproject.dto.request.RegisterRequest;
import org.chengyuan.cyproject.dto.response.AuthResponse;
import org.chengyuan.cyproject.dto.response.Response;
import org.chengyuan.cyproject.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/begain")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Response<AuthResponse> login(@RequestBody LoginRequest request) {
        return Response.ok(authService.login(request));
    }

    @PostMapping("/register")
    public Response<AuthResponse> register(@RequestBody RegisterRequest request) {
        return Response.ok(authService.register(request));
    }
}
