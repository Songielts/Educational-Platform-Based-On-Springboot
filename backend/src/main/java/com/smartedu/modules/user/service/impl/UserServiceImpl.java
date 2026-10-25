package com.smartedu.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartedu.common.constants.SystemConstants;
import com.smartedu.common.exception.BusinessException;
import com.smartedu.common.result.ResultCode;
import com.smartedu.common.utils.JwtUtils;
import com.smartedu.common.utils.RedisUtils;
import com.smartedu.modules.system.service.EmailService;
import com.smartedu.modules.user.dto.*;
import com.smartedu.modules.user.entity.Role;
import com.smartedu.modules.user.entity.User;
import com.smartedu.modules.user.entity.UserRole;
import com.smartedu.modules.user.mapper.RoleMapper;
import com.smartedu.modules.user.mapper.UserMapper;
import com.smartedu.modules.user.mapper.UserRoleMapper;
import com.smartedu.modules.user.service.UserService;
import com.smartedu.modules.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 用户Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RedisUtils redisUtils;
    private final EmailService emailService;  // 新增：邮件服务

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @Override
    public LoginVO login(UserLoginDTO loginDTO) {
        // 1. 验证验证码
        if (StringUtils.hasText(loginDTO.getCaptcha()) && StringUtils.hasText(loginDTO.getCaptchaKey())) {
            String cacheCaptcha = (String) redisUtils.get(SystemConstants.RedisKey.CAPTCHA_PREFIX + loginDTO.getCaptchaKey());
            if (cacheCaptcha == null || !cacheCaptcha.equalsIgnoreCase(loginDTO.getCaptcha())) {
                throw new BusinessException(ResultCode.CAPTCHA_ERROR);
            }
            redisUtils.delete(SystemConstants.RedisKey.CAPTCHA_PREFIX + loginDTO.getCaptchaKey());
        }

        // 2. 查询用户
        User user = userMapper.selectByUsername(loginDTO.getUsername());
        if (user == null) {
            throw new BusinessException(ResultCode.LOGIN_ERROR);
        }

        // 3. 验证密码（兼容明文和BCrypt加密）
        String storedPassword = user.getPassword();
        boolean passwordMatch;

        if (storedPassword != null && storedPassword.startsWith("$2a$")) {
            // BCrypt加密密码
            passwordMatch = passwordEncoder.matches(loginDTO.getPassword(), storedPassword);
        } else {
            // 明文密码
            passwordMatch = loginDTO.getPassword().equals(storedPassword);
        }

        if (!passwordMatch) {
            throw new BusinessException(ResultCode.LOGIN_ERROR);
        }

        // 4. 检查账号状态
        if (user.getStatus() == SystemConstants.UserStatus.DISABLED) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }

        // 5. 更新登录信息
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        // 6. 生成Token
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getUserType());

        // 7. 缓存Token到Redis
        redisUtils.set(SystemConstants.RedisKey.TOKEN_PREFIX + user.getId(), token, jwtExpiration / 1000, TimeUnit.SECONDS);

        // 8. 获取用户角色和权限
        List<String> roles = userMapper.selectUserRoles(user.getId());
        List<String> permissions = userMapper.selectUserPermissions(user.getId());

        // 9. 构建返回对象
        LoginVO loginVO = new LoginVO();
        loginVO.setAccessToken(token);
        loginVO.setTokenType("Bearer");
        loginVO.setExpiresIn(jwtExpiration / 1000);
        loginVO.setUserInfo(convertToVO(user));
        loginVO.setRoles(roles);
        loginVO.setPermissions(permissions);

        return loginVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(UserRegisterDTO registerDTO) {
        // 1. 验证两次密码是否一致
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "两次密码输入不一致");
        }

        // 2. 验证用户名是否已存在
        if (userMapper.selectByUsername(registerDTO.getUsername()) != null) {
            throw new BusinessException(ResultCode.USERNAME_ALREADY_EXIST);
        }

        // 3. 验证邮箱是否已存在
        if (userMapper.selectByEmail(registerDTO.getEmail()) != null) {
            throw new BusinessException(ResultCode.EMAIL_ALREADY_EXIST);
        }

        // 4. 验证邮箱验证码（使用 EmailService）
        if (StringUtils.hasText(registerDTO.getEmailCode())) {
            boolean valid = emailService.verifyCode(
                    registerDTO.getEmail(),
                    registerDTO.getEmailCode(),
                    "register"
            );
            if (!valid) {
                throw new BusinessException(ResultCode.CAPTCHA_ERROR.getCode(), "邮箱验证码错误或已过期");
            }
        } else {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "请输入邮箱验证码");
        }

        // 5. 创建用户
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setEmail(registerDTO.getEmail());
        user.setRealName(registerDTO.getRealName());
        user.setPhone(registerDTO.getPhone());
        user.setUserType(Integer.valueOf(registerDTO.getUserType()));
        user.setStatus(SystemConstants.UserStatus.ENABLED);
        userMapper.insert(user);

        // 6. 分配角色
        String roleCode = user.getUserType() == SystemConstants.UserType.STUDENT
                ? SystemConstants.RoleCode.ROLE_STUDENT
                : SystemConstants.RoleCode.ROLE_TEACHER;
        Role role = roleMapper.selectByRoleCode(roleCode);
        if (role != null) {
            UserRole userRole = new UserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(role.getId());
            userRoleMapper.insert(userRole);
        }

        // 7. 注册成功后清除验证码
        emailService.clearCode(registerDTO.getEmail(), "register");
    }

    @Override
    public User getByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    @Override
    public User getByEmail(String email) {
        return userMapper.selectByEmail(email);
    }

    @Override
    public UserVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        return convertToVO(user);
    }

    @Override
    public List<String> getUserRoles(Long userId) {
        return userMapper.selectUserRoles(userId);
    }

    @Override
    public List<String> getUserPermissions(Long userId) {
        return userMapper.selectUserPermissions(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserInfo(Long userId, UserUpdateDTO updateDTO) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        BeanUtils.copyProperties(updateDTO, user);
        userMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, ChangePasswordDTO passwordDTO) {
        // 1. 验证两次密码是否一致
        if (!passwordDTO.getNewPassword().equals(passwordDTO.getConfirmPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "两次密码输入不一致");
        }

        // 2. 查询用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        // 3. 验证原密码（兼容明文和BCrypt加密）
        String storedPassword = user.getPassword();
        boolean passwordMatch;

        if (storedPassword != null && storedPassword.startsWith("$2a$")) {
            // BCrypt加密密码
            passwordMatch = passwordEncoder.matches(passwordDTO.getOldPassword(), storedPassword);
        } else {
            // 明文密码
            passwordMatch = passwordDTO.getOldPassword().equals(storedPassword);
        }

        if (!passwordMatch) {
            throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR);
        }

        // 4. 修改密码
        user.setPassword(passwordEncoder.encode(passwordDTO.getNewPassword()));
        userMapper.updateById(user);

        // 5. 清除Token，强制重新登录
        redisUtils.delete(SystemConstants.RedisKey.TOKEN_PREFIX + userId);
    }

    @Override
    public void sendEmailCode(String email, String type) {
        // 使用 EmailService 发送真实邮件
        emailService.sendVerifyCode(email, type);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(ResetPasswordDTO resetDTO) {
        // 1. 验证两次密码是否一致
        if (!resetDTO.getNewPassword().equals(resetDTO.getConfirmPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "两次密码输入不一致");
        }

        // 2. 验证邮箱验证码（使用 EmailService）
        boolean valid = emailService.verifyCode(
                resetDTO.getEmail(),
                resetDTO.getEmailCode(),
                "reset"
        );
        if (!valid) {
            throw new BusinessException(ResultCode.CAPTCHA_ERROR.getCode(), "邮箱验证码错误或已过期");
        }

        // 3. 查询用户
        User user = userMapper.selectByEmail(resetDTO.getEmail());
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        // 4. 修改密码
        user.setPassword(passwordEncoder.encode(resetDTO.getNewPassword()));
        userMapper.updateById(user);

        // 5. 清除验证码
        emailService.clearCode(resetDTO.getEmail(), "reset");

        // 6. 清除Token，强制重新登录
        redisUtils.delete(SystemConstants.RedisKey.TOKEN_PREFIX + user.getId());
    }

    // ==================== 管理员功能实现 ====================

    @Override
    public IPage<UserDTO> getUserPage(UserQueryDTO queryDTO) {
        Page<User> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(User::getDeleted, 0);

        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.and(w -> w.like(User::getUsername, queryDTO.getKeyword())
                    .or().like(User::getRealName, queryDTO.getKeyword()));
        }

        if (queryDTO.getUserType() != null) {
            wrapper.eq(User::getUserType, queryDTO.getUserType());
        }

        wrapper.orderByDesc(User::getCreateTime);
        Page<User> userPage = userMapper.selectPage(page, wrapper);

        List<UserDTO> records = userPage.getRecords().stream()
                .map(this::convertToDTO)
                .toList();

        Page<UserDTO> resultPage = new Page<>();
        BeanUtils.copyProperties(userPage, resultPage);
        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleUserStatus(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        // 切换状态：1->0, 0->1
        Integer newStatus = user.getStatus() == 1 ? 0 : 1;
        user.setStatus(newStatus);
        userMapper.updateById(user);

        // 如果禁用用户，清除其登录token
        if (newStatus == 0) {
            redisUtils.delete(SystemConstants.RedisKey.TOKEN_PREFIX + userId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String resetUserPassword(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        String newPassword = "123456";
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);

        // 清除用户登录token，强制重新登录
        redisUtils.delete(SystemConstants.RedisKey.TOKEN_PREFIX + userId);
        return newPassword;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        user.setDeleted(1);
        userMapper.updateById(user);

        // 清除用户登录token
        redisUtils.delete(SystemConstants.RedisKey.TOKEN_PREFIX + userId);
    }

    // ==================== 搜索用户功能 ====================

    /**
     * 搜索用户（用于教师搜索学生）
     */
    @Override
    public List<UserVO> searchUsers(String keyword, Integer userType) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getDeleted, 0);

        // 关键词搜索（用户名或真实姓名）
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getRealName, keyword));
        }

        // 用户类型筛选
        if (userType != null) {
            wrapper.eq(User::getUserType, userType);
        }

        wrapper.orderByDesc(User::getCreateTime);
        wrapper.last("LIMIT 20");  // 最多返回20条

        List<User> users = userMapper.selectList(wrapper);

        return users.stream()
                .map(this::convertToVO)
                .toList();
    }

    /**
     * 转换为VO
     */
    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);

        // 设置用户类型名称
        switch (user.getUserType()) {
            case 1:
                vo.setUserTypeName("学生");
                break;
            case 2:
                vo.setUserTypeName("教师");
                break;
            case 3:
                vo.setUserTypeName("管理员");
                break;
            default:
                vo.setUserTypeName("未知");
        }

        // 设置角色和权限
        vo.setRoles(userMapper.selectUserRoles(user.getId()));
        vo.setPermissions(userMapper.selectUserPermissions(user.getId()));

        return vo;
    }

    /**
     * 转换为DTO
     */
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(user, dto);
        // 设置用户类型名称
        switch (user.getUserType()) {
            case 1:
                dto.setUserTypeName("学生");
                break;
            case 2:
                dto.setUserTypeName("教师");
                break;
            case 3:
                dto.setUserTypeName("管理员");
                break;
            default:
                dto.setUserTypeName("未知");
        }
        return dto;
    }
}