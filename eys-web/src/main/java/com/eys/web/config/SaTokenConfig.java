package com.eys.web.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 配置
 * 
 * 权限控制说明：
 * - 登录校验：通过 SaInterceptor 统一拦截
 * - 角色校验：通过 @SaCheckRole 注解在 Controller 方法上声明
 * - 角色层级：ADMIN > DM > PLAYER（见 StpInterfaceImpl）
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 管理端接口需要登录
            SaRouter.match("/api/admin/**")
                    .notMatch("/api/admin/auth/**")
                    .check(r -> StpUtil.checkLogin());

            // 小程序端接口需要登录（微信登录接口除外）
            SaRouter.match("/api/mp/**")
                    .notMatch("/api/mp/auth/**")
                    .check(r -> StpUtil.checkLogin());
        })).addPathPatterns("/**")
                .excludePathPatterns(
                        "/doc.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/webjars/**",
                        "/favicon.ico");
    }
}


