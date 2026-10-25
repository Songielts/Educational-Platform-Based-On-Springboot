package com.smartedu.modules.classroom.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smartedu.modules.classroom.dto.ClassDTO;
import com.smartedu.modules.classroom.dto.ClassStudentVO;
import com.smartedu.modules.classroom.dto.ClassVO;
import com.smartedu.modules.classroom.entity.Class;

import java.util.List;

/**
 * 班级Service接口
 */
public interface ClassService extends IService<Class> {

    /**
     * 创建班级
     */
    void createClass(Long teacherId, ClassDTO classDTO);

    /**
     * 更新班级
     */
    void updateClass(Long teacherId, ClassDTO classDTO);

    /**
     * 删除班级
     */
    void deleteClass(Long teacherId, Long classId);

    /**
     * 获取班级详情
     */
    ClassVO getClassDetail(Long classId);

    /**
     * 分页查询教师的班级列表
     */
    Page<ClassVO> getTeacherClassPage(Long teacherId, String keyword, Long current, Long size);

    /**
     * 生成班级加入码
     */
    String generateJoinCode(Long teacherId, Long classId, Integer expireHours);

    /**
     * 学生加入班级
     */
    void joinClass(Long studentId, String joinCode);

    /**
     * 学生退出班级
     */
    void leaveClass(Long studentId, Long classId);

    /**
     * 批量导入学生
     */
    void batchImportStudents(Long teacherId, Long classId, List<Long> studentIds);

    /**
     * 移除班级学生
     */
    void removeStudent(Long teacherId, Long classId, Long studentId);

    /**
     * 获取班级的学生列表
     */
    List<Long> getClassStudentIds(Long classId);

    /**
     * 检查学生是否在班级中
     */
    boolean isStudentInClass(Long classId, Long studentId);

    /**
     * 获取学生加入的班级列表
     */
    List<ClassVO> getStudentClasses(Long studentId);
    /**
     * 获取班级学生列表
     */
    List<ClassStudentVO> getClassStudents(Long classId);

    /**
     * 添加学生到班级
     */
    void addStudentToClass(Long teacherId, Long classId, Long studentId);

    /**
     * 从班级移除学生
     */
    void removeStudentFromClass(Long teacherId, Long classId, Long studentId);
    /**
     * 检查学生是否加入了任何班级
     */
    boolean isStudentInAnyClass(Long studentId);

}
