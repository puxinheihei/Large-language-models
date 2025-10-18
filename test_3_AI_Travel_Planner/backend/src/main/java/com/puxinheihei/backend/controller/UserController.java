package com.puxinheihei.backend.controller;

import com.puxinheihei.backend.model.User;
import com.puxinheihei.backend.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Map<String, String> register(@RequestBody User user) {
        return userService.register(user)
                .map(u -> Map.of("status", "ok", "userId", u.getId()))
                .orElseGet(() -> Map.of("status", "error", "message", "用户名已存在"));
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        return userService.login(username, password)
                .map(u -> Map.of("status", "ok", "userId", u.getId()))
                .orElseGet(() -> Map.of("status", "error", "message", "用户名或密码错误"));
    }
}