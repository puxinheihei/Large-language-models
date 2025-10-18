package com.puxinheihei.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.puxinheihei.backend.mapper.UserMapper;
import com.puxinheihei.backend.model.User;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public Optional<User> findByUsername(String username) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        qw.eq(User::getUsername, username).last("LIMIT 1");
        return Optional.ofNullable(userMapper.selectOne(qw));
    }

    public Optional<User> register(User user) {
        if (findByUsername(user.getUsername()).isPresent()) {
            return Optional.empty();
        }
        user.setId(UUID.randomUUID().toString());
        userMapper.insert(user);
        return Optional.of(user);
    }

    public Optional<User> login(String username, String password) {
        Optional<User> u = findByUsername(username);
        if (u.isPresent() && password != null && password.equals(u.get().getPassword())) {
            return u;
        }
        return Optional.empty();
    }
}