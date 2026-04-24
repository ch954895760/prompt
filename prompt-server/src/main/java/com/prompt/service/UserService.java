package com.prompt.service;

import com.prompt.dto.LoginRequest;
import com.prompt.dto.RegisterRequest;
import com.prompt.entity.User;
import com.prompt.entity.UserSetting;
import com.prompt.exception.BusinessException;
import com.prompt.mapper.UserMapper;
import com.prompt.mapper.UserSettingMapper;
import com.prompt.util.JwtUtil;
import com.prompt.vo.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final UserSettingMapper userSettingMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public Map<String, Object> register(RegisterRequest request) {
        User existingUser = userMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .eq(User::getEmail, request.getEmail())
        );
        if (existingUser != null) {
            throw new BusinessException("邮箱已被注册");
        }

        User existingUsername = userMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())
        );
        if (existingUsername != null) {
            throw new BusinessException("用户名已被使用");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userMapper.insert(user);

        UserSetting setting = new UserSetting();
        setting.setUserId(user.getId());
        setting.setTheme("light");
        userSettingMapper.insert(setting);

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshToken);
        result.put("user", user);
        return result;
    }

    public Map<String, Object> login(LoginRequest request) {
        User user = userMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .eq(User::getEmail, request.getEmail())
        );
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(401, "邮箱或密码错误");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken;
        if (Boolean.TRUE.equals(request.getRememberMe())) {
            refreshToken = jwtUtil.generateRememberMeToken(user.getId(), user.getUsername());
        } else {
            refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshToken);
        result.put("user", user);
        result.put("rememberMe", request.getRememberMe());
        return result;
    }

    public Map<String, Object> refreshToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BusinessException(401, "刷新令牌无效或已过期");
        }

        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }

        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", newAccessToken);
        result.put("refreshToken", newRefreshToken);
        return result;
    }

    public User getCurrentUser(Long userId) {
        return userMapper.selectById(userId);
    }
}
