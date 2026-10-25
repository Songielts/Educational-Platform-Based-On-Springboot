package com.smartedu.modules.course.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartedu.modules.course.entity.StudentCourse;
import com.smartedu.modules.course.mapper.StudentCourseMapper;
import com.smartedu.modules.course.service.StudentCourseService;
import org.springframework.stereotype.Service;

@Service
public class StudentCourseServiceImpl extends ServiceImpl<StudentCourseMapper, StudentCourse>
        implements StudentCourseService {

    @Override
    public boolean isSelected(Long studentId, Long courseId) {
        return baseMapper.countByStudentAndCourse(studentId, courseId) > 0;
    }
}