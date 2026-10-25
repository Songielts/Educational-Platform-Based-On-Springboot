package com.smartedu.modules.classroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartedu.modules.classroom.entity.Class;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 班级Mapper接口
 */
public interface ClassMapper extends BaseMapper<Class> {

    /**
     * 根据班级编码查询
     */
    @Select("SELECT * FROM edu_class WHERE class_code = #{classCode} AND deleted = 0")
    Class selectByClassCode(@Param("classCode") String classCode);

    /**
     * 根据加入码查询
     */
    @Select("SELECT * FROM edu_class WHERE join_code = #{joinCode} AND deleted = 0")
    Class selectByJoinCode(@Param("joinCode") String joinCode);

    /**
     * 分页查询教师的班级列表
     */
    IPage<Class> selectTeacherClassPage(Page<Class> page, @Param("teacherId") Long teacherId,
                                        @Param("keyword") String keyword);

    /**
     * 查询学生加入的班级列表
     */
    List<Class> selectStudentClasses(@Param("studentId") Long studentId);

    /**
     * 增加班级学生数
     */
    @Update("UPDATE edu_class SET current_students = current_students + 1 WHERE id = #{classId}")
    int increaseStudentCount(@Param("classId") Long classId);

    /**
     * 减少班级学生数
     */
    @Update("UPDATE edu_class SET current_students = current_students - 1 WHERE id = #{classId}")
    int decreaseStudentCount(@Param("classId") Long classId);

}