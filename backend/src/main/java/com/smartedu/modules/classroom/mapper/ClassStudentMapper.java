package com.smartedu.modules.classroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartedu.modules.classroom.entity.ClassStudent;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 班级学生关联Mapper接口
 */
public interface ClassStudentMapper extends BaseMapper<ClassStudent> {

    /**
     * 检查学生是否在班级中
     */
    @Select("SELECT COUNT(*) FROM edu_class_student WHERE class_id = #{classId} AND student_id = #{studentId} AND status = 1 AND deleted = 0")
    int checkStudentInClass(@Param("classId") Long classId, @Param("studentId") Long studentId);

    /**
     * 根据班级ID查询学生列表
     */
    @Select("SELECT * FROM edu_class_student WHERE class_id = #{classId} AND deleted = 0")
    List<ClassStudent> selectByClassId(@Param("classId") Long classId);

    /**
     * 查询班级学生ID列表
     */
    @Select("SELECT student_id FROM edu_class_student WHERE class_id = #{classId} AND deleted = 0")
    List<Long> selectStudentIdsByClassId(@Param("classId") Long classId);

    /**
     * 统计班级学生数
     */
    @Select("SELECT COUNT(*) FROM edu_class_student WHERE class_id = #{classId} AND deleted = 0")
    int countStudentsByClassId(@Param("classId") Long classId);

    /**
     * 根据班级ID和学生ID查询关联（包括已删除的）
     */
    @Select("SELECT * FROM edu_class_student WHERE class_id = #{classId} AND student_id = #{studentId}")
    ClassStudent selectByClassIdAndStudentId(@Param("classId") Long classId, @Param("studentId") Long studentId);

    /**
     * 查询学生是否加入了任何班级
     */
    @Select("SELECT COUNT(*) FROM edu_class_student WHERE student_id = #{studentId} AND status = 1 AND deleted = 0")
    int checkStudentInAnyClass(@Param("studentId") Long studentId);
}