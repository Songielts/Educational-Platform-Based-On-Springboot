package com.smartedu.modules.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smartedu.modules.user.dto.*;
import com.smartedu.modules.user.entity.User;
import com.smartedu.modules.user.dto.LoginVO;
import com.smartedu.modules.user.vo.UserVO;

import java.util.List;

/**
 * 用户Service接口
 */
public interface UserService extends IService<User> {

    /**
     * 用户登录
     */
    LoginVO login(UserLoginDTO loginDTO);

    /**
     * 用户注册
     */
    void register(UserRegisterDTO registerDTO);

    /**
     * 根据用户名获取用户
     */
    User getByUsername(String username);

    /**
     * 根据邮箱获取用户
     */
    User getByEmail(String email);

    /**
     * 获取用户信息 - 返回 UserVO
     */
    UserVO getUserInfo(Long userId);

    /**
     * 获取用户角色列表
     */
    List<String> getUserRoles(Long userId);

    /**
     * 获取用户权限列表
     */
    List<String> getUserPermissions(Long userId);

    /**
     * 更新用户信息
     */
    void updateUserInfo(Long userId, UserUpdateDTO updateDTO);

    /**
     * 修改密码
     */
    void changePassword(Long userId, ChangePasswordDTO passwordDTO);

    /**
     * 发送邮箱验证码
     */
    void sendEmailCode(String email, String type);

    /**
     * 重置密码（通过邮箱验证码）
     */
    void resetPassword(ResetPasswordDTO resetDTO);

    /**
     * 搜索用户
     */
    List<UserVO> searchUsers(String keyword, Integer userType);
    // ==================== 管理员功能 ====================

    /**
     * 分页查询用户列表
     */
    IPage<UserDTO> getUserPage(UserQueryDTO queryDTO);

    /**
     * 切换用户状态（启用/禁用）
     */
    void toggleUserStatus(Long userId);

    /**
     * 重置用户密码（管理员）
     */
    String resetUserPassword(Long userId);

    /**
     * 删除用户
     */
    void deleteUser(Long userId);
}