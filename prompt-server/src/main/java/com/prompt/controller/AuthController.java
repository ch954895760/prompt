package com.prompt.controller;

import com.prompt.dto.ChangePasswordRequest;
import com.prompt.dto.LoginRequest;
import com.prompt.dto.RefreshTokenRequest;
import com.prompt.dto.RegisterRequest;
import com.prompt.entity.User;
import com.prompt.service.UserService;
import com.prompt.util.JwtUtil;
import com.prompt.vo.Result;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

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

    @PutMapping("/avatar")
    public Result<Void> updateAvatar(@RequestBody Map<String, String> request,
                                     @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);
        String avatarUrl = request.get("avatarUrl");
        userService.updateAvatar(userId, avatarUrl);
        return Result.success();
    }

    @GetMapping("/me")
    public Result<User> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);
        return Result.success(userService.getCurrentUser(userId));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                       @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);
        userService.changePassword(userId, request);
        return Result.success();
    }
}

