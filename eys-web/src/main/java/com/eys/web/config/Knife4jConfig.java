package com.eys.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j/OpenAPI 配置
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("鹅鸭杀面杀辅助工具 API")
                        .description("线下鹅鸭杀面杀辅助工具后端接口文档")
                        .version("1.0.0")
                        .contact(new Contact().name("EYS Team")));
    }

    /**
     * 小程序端 API 分组
     */
    @Bean
    public GroupedOpenApi miniappApi() {
        return GroupedOpenApi.builder()
                .group("1-小程序端")
                .displayName("小程序端 API")
                .packagesToScan("com.eys.miniapp.controller")
                .build();
    }

    /**
     * 管理员端 API 分组
     */
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("2-管理员端")
                .displayName("管理员端 API")
                .packagesToScan("com.eys.admin.controller")
                .build();
    }
}
