package com.smartedu.security.config;

import com.smartedu.security.filter.JwtAuthenticationFilter;
import com.smartedu.security.handler.CustomAuthenticationEntryPoint;
import com.smartedu.security.handler.CustomAccessDeniedHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置
 *
 * 修改说明：
 * - 移除 WebSecurity.ignoring("/images/**")，避免与 WebMvcConfig.addResourceHandlers 冲突
 * - 图片放行改由 JwtAuthenticationFilter 白名单 + permitAll() 双重保障
 * - 请求走完整 Spring MVC 流程，WebMvcConfig 资源映射才能生效
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder());
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        // 完全忽略这些路径的 security 检查
        // 注意：ignoring() 会绕过整个 Spring Security 过滤器链，包括 DispatcherServlet
        // 因此不能再放 /images/**，否则 WebMvcConfig.addResourceHandlers 不生效
        web.ignoring()
                .antMatchers("/uploads/**");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .cors().and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                // 允许匿名访问的路径（走完整过滤器链，JwtAuthenticationFilter 白名单先放行）
                .antMatchers("/auth/**", "/captcha/**", "/upload/**", "/public/**",
                        "/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**",
                        "/file/video/stream/**",
                        "/images/**",           // <-- 图片资源：permitAll + Filter 白名单
                        "/uploads/**"            // <-- 上传资源
                ).permitAll()
                // === 放行前端 blob 请求的图片资源路径 ===
                .antMatchers("/api/uploads/**").permitAll()
                // 其他请求需要认证
                .anyRequest().authenticated()
                .and()
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling()
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler);
    }
}