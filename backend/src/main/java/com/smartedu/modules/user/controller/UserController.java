package com.smartedu.modules.user.controller;

import com.smartedu.common.result.Result;
import com.smartedu.modules.user.dto.ChangePasswordDTO;
import com.smartedu.modules.user.dto.UserUpdateDTO;
import com.smartedu.modules.user.entity.User;
import com.smartedu.modules.user.service.UserService;
import com.smartedu.modules.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    public Result<UserVO> getUserInfo() {
        Long userId = getCurrentUserId();
        log.info("[getUserInfo] userId: {}", userId);
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        UserVO userVO = userService.getUserInfo(userId);
        return Result.success(userVO);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/info")
    public Result<Void> updateUserInfo(@RequestBody UserUpdateDTO updateDTO) {
        Long userId = getCurrentUserId();
        log.info("[updateUserInfo] userId: {}", userId);
        if (userId == null) {
            return Result.error(401, "未登录或无法获取用户ID");
        }
        userService.updateUserInfo(userId, updateDTO);
        return Result.success("更新成功");
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody ChangePasswordDTO passwordDTO) {
        Long userId = getCurrentUserId();
        log.info("[changePassword] userId: {}", userId);
        if (userId == null) {
            return Result.error(401, "未登录或无法获取用户ID");
        }
        userService.changePassword(userId, passwordDTO);
        return Result.success("密码修改成功");
    }

    /**
     * 搜索用户（教师搜索学生）
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<List<UserVO>> searchUsers(@RequestParam String keyword,
                                            @RequestParam(required = false) Integer userType) {
        List<UserVO> users = userService.searchUsers(keyword, userType);
        return Result.success(users);
    }

    /**
     * 从 SecurityContext 提取当前登录用户ID
     * JwtAuthenticationFilter 中 principal 是 Long 类型的 userId
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("[getCurrentUserId] authentication is null or not authenticated");
            return null;
        }

        Object principal = authentication.getPrincipal();
        log.info("[getCurrentUserId] principal type: {}, value: {}",
                principal != null ? principal.getClass().getName() : "null", principal);

        if (principal == null) {
            return null;
        }

        // JwtAuthenticationFilter 中 principal 就是 Long 类型的 userId
        if (principal instanceof Long) {
            return (Long) principal;
        }

        // 兼容 String（如果 Filter 改为存用户名）
        if (principal instanceof String) {
            try {
                return Long.valueOf((String) principal);
            } catch (NumberFormatException e) {
                log.warn("[getCurrentUserId] 无法将 String 转为 Long: {}", principal);
                return null;
            }
        }

        // 兼容 UserDetails
        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            User user = userService.getByUsername(username);
            return user != null ? user.getId() : null;
        }

        // 兼容 User 实体
        if (principal instanceof com.smartedu.modules.user.entity.User) {
            return ((com.smartedu.modules.user.entity.User) principal).getId();
        }

        log.warn("[getCurrentUserId] 不支持的 principal 类型: {}", principal.getClass().getName());
        return null;
    }
}