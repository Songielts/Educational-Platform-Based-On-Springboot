package com.smartedu.modules.classroom.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartedu.common.constants.SystemConstants;
import com.smartedu.common.exception.BusinessException;
import com.smartedu.common.result.ResultCode;
import com.smartedu.modules.classroom.dto.ClassDTO;
import com.smartedu.modules.classroom.dto.ClassStudentVO;
import com.smartedu.modules.classroom.dto.ClassVO;
import com.smartedu.modules.classroom.entity.Class;
import com.smartedu.modules.classroom.entity.ClassStudent;
import com.smartedu.modules.classroom.mapper.ClassMapper;
import com.smartedu.modules.classroom.mapper.ClassStudentMapper;
import com.smartedu.modules.classroom.service.ClassService;
import com.smartedu.modules.user.entity.User;
import com.smartedu.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 班级Service实现类
 */
@Service
@RequiredArgsConstructor
public class ClassServiceImpl extends ServiceImpl<ClassMapper, Class> implements ClassService {

    private final ClassMapper classMapper;
    private final ClassStudentMapper classStudentMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createClass(Long teacherId, ClassDTO classDTO) {
        // 1. 检查班级编码是否已存在
        if (classMapper.selectByClassCode(classDTO.getClassCode()) != null) {
            throw new BusinessException(ResultCode.CLASS_CODE_ALREADY_EXIST);
        }

        // 2. 创建班级
        Class clazz = new Class();
        BeanUtils.copyProperties(classDTO, clazz);
        clazz.setTeacherId(teacherId);
        clazz.setCurrentStudents(0);
        clazz.setStatus(SystemConstants.ClassStatus.OPEN);
        classMapper.insert(clazz);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateClass(Long teacherId, ClassDTO classDTO) {
        // 1. 查询班级
        Class clazz = classMapper.selectById(classDTO.getId());
        if (clazz == null) {
            throw new BusinessException(ResultCode.CLASS_NOT_EXIST);
        }

        // 2. 验证是否是该教师的班级
        if (!clazz.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        // 3. 如果修改了班级编码，检查是否已存在
        if (!clazz.getClassCode().equals(classDTO.getClassCode())) {
            if (classMapper.selectByClassCode(classDTO.getClassCode()) != null) {
                throw new BusinessException(ResultCode.CLASS_CODE_ALREADY_EXIST);
            }
        }

        // 4. 更新班级
        BeanUtils.copyProperties(classDTO, clazz);
        classMapper.updateById(clazz);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteClass(Long teacherId, Long classId) {
        // 1. 查询班级
        Class clazz = classMapper.selectById(classId);
        if (clazz == null) {
            throw new BusinessException(ResultCode.CLASS_NOT_EXIST);
        }

        // 2. 验证是否是该教师的班级
        if (!clazz.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        // 3. 删除班级（逻辑删除）
        classMapper.deleteById(classId);

        // 4. 删除班级学生关联
        classStudentMapper.deleteById(classId);
    }

    @Override
    public ClassVO getClassDetail(Long classId) {
        Class clazz = classMapper.selectById(classId);
        if (clazz == null) {
            throw new BusinessException(ResultCode.CLASS_NOT_EXIST);
        }
        return convertToVO(clazz);
    }

    @Override
    public Page<ClassVO> getTeacherClassPage(Long teacherId, String keyword, Long current, Long size) {
        System.out.println("=== 查询教师班级列表 ===");
        System.out.println("teacherId: " + teacherId);
        System.out.println("keyword: " + keyword);
        System.out.println("current: " + current);
        System.out.println("size: " + size);

        Page<Class> page = new Page<>(current, size);
        Page<Class> classPage = (Page<Class>) classMapper.selectTeacherClassPage(page, teacherId, keyword);

        System.out.println("查询结果条数: " + classPage.getRecords().size());
        System.out.println("查询结果: " + classPage.getRecords());

        List<ClassVO> records = classPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        Page<ClassVO> resultPage = new Page<>();
        BeanUtils.copyProperties(classPage, resultPage);
        resultPage.setRecords(records);

        return resultPage;
    }

    /**
     * 获取班级学生列表
     */
    @Override
    public List<ClassStudentVO> getClassStudents(Long classId) {
        // 1. 查询班级是否存在
        Class clazz = classMapper.selectById(classId);
        if (clazz == null) {
            throw new BusinessException(ResultCode.CLASS_NOT_EXIST);
        }

        // 2. 查询班级学生关联
        List<ClassStudent> classStudents = classStudentMapper.selectByClassId(classId);

        // 3. 转换为VO
        return classStudents.stream().map(cs -> {
            ClassStudentVO vo = new ClassStudentVO();
            vo.setStudentId(cs.getStudentId());
            vo.setJoinTime(cs.getJoinTime());

            // 查询学生信息
            User student = userMapper.selectById(cs.getStudentId());
            if (student != null) {
                vo.setUsername(student.getUsername());
                vo.setRealName(student.getRealName());
                vo.setAvatar(student.getAvatar());
            }

            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 添加学生到班级
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addStudentToClass(Long teacherId, Long classId, Long studentId) {
        // 1. 查询班级
        Class clazz = classMapper.selectById(classId);
        if (clazz == null) {
            throw new BusinessException(ResultCode.CLASS_NOT_EXIST);
        }

        // 2. 验证是否是该教师的班级
        if (!clazz.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        // 3. 检查学生是否存在
        User student = userMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        if (student.getUserType() != SystemConstants.UserType.STUDENT) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "只能添加学生用户");
        }

        // 4. 检查学生是否已在班级中（包括已删除的）
        ClassStudent existing = classStudentMapper.selectByClassIdAndStudentId(classId, studentId);
        if (existing != null) {
            if (existing.getDeleted() == 0) {
                // 学生已在班级中
                throw new BusinessException(ResultCode.STUDENT_ALREADY_IN_CLASS);
            } else {
                // 已删除的记录，恢复它
                existing.setDeleted(0);
                existing.setStatus(1);
                existing.setJoinTime(LocalDateTime.now());
                classStudentMapper.updateById(existing);

                // 更新班级学生数
                classMapper.increaseStudentCount(classId);
                return;
            }
        }

        // 5. 检查班级是否已满
        if (clazz.getCurrentStudents() >= clazz.getMaxStudents()) {
            throw new BusinessException(ResultCode.CLASS_FULL);
        }

        // 6. 添加学生到班级
        ClassStudent classStudent = new ClassStudent();
        classStudent.setClassId(classId);
        classStudent.setStudentId(studentId);
        classStudent.setStatus(1);
        classStudent.setJoinTime(LocalDateTime.now());
        classStudent.setDeleted(0);
        classStudentMapper.insert(classStudent);

        // 7. 更新班级学生数
        classMapper.increaseStudentCount(classId);
    }
    /**
     * 从班级移除学生
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeStudentFromClass(Long teacherId, Long classId, Long studentId) {
        // 1. 查询班级
        Class clazz = classMapper.selectById(classId);
        if (clazz == null) {
            throw new BusinessException(ResultCode.CLASS_NOT_EXIST);
        }

        // 2. 验证是否是该教师的班级
        if (!clazz.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        // 3. 检查学生是否在班级中
        if (classStudentMapper.checkStudentInClass(classId, studentId) == 0) {
            throw new BusinessException(ResultCode.STUDENT_NOT_IN_CLASS);
        }

        // 4. 删除关联
        classStudentMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getClassId, classId)
                .eq(ClassStudent::getStudentId, studentId));

        // 5. 更新班级学生数
        classMapper.decreaseStudentCount(classId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String generateJoinCode(Long teacherId, Long classId, Integer expireHours) {
        // 1. 查询班级
        Class clazz = classMapper.selectById(classId);
        if (clazz == null) {
            throw new BusinessException(ResultCode.CLASS_NOT_EXIST);
        }

        // 2. 验证是否是该教师的班级
        if (!clazz.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        // 3. 生成加入码
        String joinCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 4. 更新班级加入码
        clazz.setJoinCode(joinCode);
        clazz.setJoinCodeExpire(LocalDateTime.now().plusHours(expireHours));
        classMapper.updateById(clazz);

        return joinCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void joinClass(Long studentId, String joinCode) {
        // 1. 根据加入码查询班级
        Class clazz = classMapper.selectByJoinCode(joinCode);
        if (clazz == null) {
            throw new BusinessException(ResultCode.INVALID_JOIN_CODE);
        }

        // 2. 检查加入码是否过期
        if (clazz.getJoinCodeExpire() != null && clazz.getJoinCodeExpire().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.JOIN_CODE_EXPIRED);
        }

        // 3. 检查班级状态
        if (clazz.getStatus() == SystemConstants.ClassStatus.CLOSED) {
            throw new BusinessException(ResultCode.CLASS_NOT_EXIST.getCode(), "班级已关闭");
        }

        // 4. 检查班级是否已满
        if (clazz.getCurrentStudents() >= clazz.getMaxStudents()) {
            throw new BusinessException(ResultCode.CLASS_FULL);
        }

        // 5. 检查学生是否已在班级中
        if (classStudentMapper.checkStudentInClass(clazz.getId(), studentId) > 0) {
            throw new BusinessException(ResultCode.STUDENT_ALREADY_IN_CLASS);
        }

        // 6. 添加学生到班级
        ClassStudent classStudent = new ClassStudent();
        classStudent.setClassId(clazz.getId());
        classStudent.setStudentId(studentId);
        classStudent.setStatus(1);
        classStudentMapper.insert(classStudent);

        // 7. 更新班级学生数
        classMapper.increaseStudentCount(clazz.getId());
    }
    //是否在班级中，判断是能看视频
    @Override
    public boolean isStudentInAnyClass(Long studentId) {
        return classStudentMapper.checkStudentInAnyClass(studentId) > 0;
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveClass(Long studentId, Long classId) {
        // 1. 检查学生是否在班级中
        if (classStudentMapper.checkStudentInClass(classId, studentId) == 0) {
            throw new BusinessException(ResultCode.STUDENT_NOT_IN_CLASS);
        }

        // 2. 删除关联
        classStudentMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getClassId, classId)
                .eq(ClassStudent::getStudentId, studentId));

        // 3. 更新班级学生数
        classMapper.decreaseStudentCount(classId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchImportStudents(Long teacherId, Long classId, List<Long> studentIds) {
        // 1. 查询班级
        Class clazz = classMapper.selectById(classId);
        if (clazz == null) {
            throw new BusinessException(ResultCode.CLASS_NOT_EXIST);
        }

        // 2. 验证是否是该教师的班级
        if (!clazz.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        // 3. 批量添加学生
        for (Long studentId : studentIds) {
            // 检查学生是否已在班级中
            if (classStudentMapper.checkStudentInClass(classId, studentId) > 0) {
                continue;
            }

            ClassStudent classStudent = new ClassStudent();
            classStudent.setClassId(classId);
            classStudent.setStudentId(studentId);
            classStudent.setStatus(1);
            classStudentMapper.insert(classStudent);
        }

        // 4. 更新班级学生数
        int count = classStudentMapper.countStudentsByClassId(classId);
        clazz.setCurrentStudents(count);
        classMapper.updateById(clazz);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeStudent(Long teacherId, Long classId, Long studentId) {
        // 1. 查询班级
        Class clazz = classMapper.selectById(classId);
        if (clazz == null) {
            throw new BusinessException(ResultCode.CLASS_NOT_EXIST);
        }

        // 2. 验证是否是该教师的班级
        if (!clazz.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        // 3. 删除关联
        classStudentMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getClassId, classId)
                .eq(ClassStudent::getStudentId, studentId));

        // 4. 更新班级学生数
        classMapper.decreaseStudentCount(classId);
    }

    @Override
    public List<Long> getClassStudentIds(Long classId) {
        return classStudentMapper.selectStudentIdsByClassId(classId);
    }

    @Override
    public boolean isStudentInClass(Long classId, Long studentId) {
        return classStudentMapper.checkStudentInClass(classId, studentId) > 0;
    }

    @Override
    public List<ClassVO> getStudentClasses(Long studentId) {
        List<Class> classes = classMapper.selectStudentClasses(studentId);
        return classes.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 转换为VO
     */
    private ClassVO convertToVO(Class clazz) {
        ClassVO vo = new ClassVO();
        BeanUtils.copyProperties(clazz, vo);

        // 查询教师名称
        User teacher = userMapper.selectById(clazz.getTeacherId());
        if (teacher != null) {
            vo.setTeacherName(teacher.getRealName());
        }

        return vo;
    }
}