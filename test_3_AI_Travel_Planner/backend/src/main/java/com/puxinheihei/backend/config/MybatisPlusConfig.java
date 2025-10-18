package com.puxinheihei.backend.config;

import org.springframework.context.annotation.Configuration;
import org.mybatis.spring.annotation.MapperScan;

@Configuration
@MapperScan("com.puxinheihei.backend.mapper")
public class MybatisPlusConfig {
}