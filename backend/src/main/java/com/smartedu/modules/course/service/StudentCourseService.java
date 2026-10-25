package com.smartedu.modules.course.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartedu.modules.course.entity.StudentCourse;

public interface StudentCourseService extends IService<StudentCourse> {
    boolean isSelected(Long studentId, Long courseId);
}