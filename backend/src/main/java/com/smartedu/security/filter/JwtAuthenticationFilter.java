package com.smartedu.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartedu.common.constants.SystemConstants;
import com.smartedu.common.result.Result;
import com.smartedu.common.utils.JwtUtils;
import com.smartedu.common.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT认证过滤器
 *
 * 修复说明：
 * - 认证失败时返回401 JSON响应，不再直接放行
 * - 避免token过期后请求继续走下去导致404/403混淆
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final RedisUtils redisUtils;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 白名单路径（不需要 JWT 认证的请求）
     */
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/api/auth/",
            "/api/captcha/",
            "/api/upload/",
            "/api/public/",
            "/api/swagger-ui/",
            "/api/v3/api-docs/",
            "/api/swagger-resources/",
            "/api/file/video/stream/",
            "/api/images/",
            "/api/uploads/",
            "/api/error"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        for (String whitePath : WHITE_LIST) {
            if (requestUri.startsWith(whitePath)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. 获取请求头中的Authorization
        String authHeader = request.getHeader("Authorization");

        // 2. 判断是否有token
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            writeUnauthorized(response, 401, "未登录，请先登录");
            return;
        }

        // 3. 提取token
        String token = authHeader.substring(7);

        try {
            // 4. 验证token
            if (!jwtUtils.validateToken(token)) {
                writeUnauthorized(response, 401, "登录已过期，请重新登录");
                return;
            }

            // 5. 获取用户ID
            Long userId = jwtUtils.getUserId(token);
            if (userId == null) {
                writeUnauthorized(response, 401, "token解析失败");
                return;
            }

            // 6. 验证token是否在Redis中存在
            String cacheToken = (String) redisUtils.get(SystemConstants.RedisKey.TOKEN_PREFIX + userId);
            if (!token.equals(cacheToken)) {
                writeUnauthorized(response, 401, "token已失效，请重新登录");
                return;
            }

            // 7. 获取用户角色
            List<String> roles = jwtUtils.getUserType(token) == SystemConstants.UserType.ADMIN
                    ? List.of(SystemConstants.RoleCode.ROLE_ADMIN)
                    : jwtUtils.getUserType(token) == SystemConstants.UserType.TEACHER
                    ? List.of(SystemConstants.RoleCode.ROLE_TEACHER)
                    : List.of(SystemConstants.RoleCode.ROLE_STUDENT);

            // 8. 构建认证对象
            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // 9. 设置安全上下文
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            log.error("JWT认证失败: {}", e.getMessage());
            writeUnauthorized(response, 401, "认证失败：" + e.getMessage());
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 关键修复：认证失败时返回401 JSON，不再直接放行
     */
    private void writeUnauthorized(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        Result<?> result = Result.error(code, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}