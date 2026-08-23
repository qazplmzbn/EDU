package com.xyz.question_bank_management_system.modules.user.controller;

import com.xyz.question_bank_management_system.common.ApiResponse;
import com.xyz.question_bank_management_system.modules.user.dto.LoginRequest;
import com.xyz.question_bank_management_system.modules.user.vo.LoginResponse;
import com.xyz.question_bank_management_system.modules.user.dto.RegisterRequest;
import com.xyz.question_bank_management_system.modules.user.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public LoginResponse register(@RequestBody @Valid RegisterRequest request, HttpServletRequest http) {
        String ip = http.getRemoteAddr();
        String ua = http.getHeader("User-Agent");
        return authService.register(request, ip, ua);
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginRequest request, HttpServletRequest http) {
        String ip = http.getRemoteAddr();
        String ua = http.getHeader("User-Agent");
        return authService.login(request, ip, ua);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.ok();
    }

    @GetMapping("/auth/me")
    public ApiResponse<LoginResponse.UserDTO> me() {
        return ApiResponse.ok(authService.me());
    }
}
