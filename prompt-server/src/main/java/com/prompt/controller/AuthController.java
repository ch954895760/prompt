package com.prompt.controller;

import com.prompt.dto.LoginRequest;
import com.prompt.dto.RefreshTokenRequest;
import com.prompt.dto.RegisterRequest;
import com.prompt.service.UserService;
import com.prompt.vo.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(userService.register(request));
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(userService.login(request));
    }

    @PostMapping("/refresh")
    public Result<Map<String, Object>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return Result.success(userService.refreshToken(request.getRefreshToken()));
    }
}
