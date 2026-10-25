package com.smartedu.modules.classroom.controller;

import com.smartedu.common.result.PageResult;
import com.smartedu.common.result.Result;
import com.smartedu.modules.classroom.dto.ClassDTO;
import com.smartedu.modules.classroom.dto.ClassStudentVO;
import com.smartedu.modules.classroom.dto.ClassVO;
import com.smartedu.modules.classroom.service.ClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 班级控制器
 */
@RestController
@RequestMapping("/class")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;

    /**
     * 创建班级（教师）
     */
    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Void> createClass(@AuthenticationPrincipal Long teacherId,
                                    @RequestBody @Validated ClassDTO classDTO) {
        classService.createClass(teacherId, classDTO);
        return Result.success("创建成功");
    }

    /**
     * 更新班级（教师）
     */
    @PutMapping("/{classId}")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Void> updateClass(@AuthenticationPrincipal Long teacherId,
                                    @PathVariable Long classId,
                                    @RequestBody @Validated ClassDTO classDTO) {
        classDTO.setId(classId);
        classService.updateClass(teacherId, classDTO);
        return Result.success("更新成功");
    }

    /**
     * 删除班级（教师）
     */
    @DeleteMapping("/{classId}")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Void> deleteClass(@AuthenticationPrincipal Long teacherId,
                                    @PathVariable Long classId) {
        classService.deleteClass(teacherId, classId);
        return Result.success("删除成功");
    }

    /**
     * 获取班级详情
     */
    @GetMapping("/{classId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public Result<ClassVO> getClassDetail(@PathVariable Long classId) {
        ClassVO classVO = classService.getClassDetail(classId);
        return Result.success(classVO);
    }

    /**
     * 分页查询教师的班级列表
     */
    @GetMapping("/teacher/list")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<PageResult<ClassVO>> getTeacherClassList(
            @AuthenticationPrincipal Long teacherId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {

        System.out.println("=== 获取教师班级列表 ===");
        System.out.println("teacherId: " + teacherId);
        System.out.println("keyword: " + keyword);
        System.out.println("current: " + current);
        System.out.println("size: " + size);

        var page = classService.getTeacherClassPage(teacherId, keyword, current, size);

        System.out.println("查询结果: " + page.getRecords().size() + " 条");
        System.out.println("总记录数: " + page.getTotal());

        return Result.success(PageResult.of(
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                page.getRecords()
        ));
    }

    /**
     * 生成班级加入码（教师）
     */
    @PostMapping("/{classId}/join-code")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<String> generateJoinCode(@AuthenticationPrincipal Long teacherId,
                                           @PathVariable Long classId,
                                           @RequestParam(defaultValue = "24") Integer expireHours) {
        String joinCode = classService.generateJoinCode(teacherId, classId, expireHours);
        return Result.success("生成成功", joinCode);
    }

    /**
     * 获取班级学生列表
     */
    @GetMapping("/{classId}/students")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public Result<List<ClassStudentVO>> getClassStudents(@PathVariable Long classId) {
        List<ClassStudentVO> students = classService.getClassStudents(classId);
        return Result.success(students);
    }

    /**
     * 添加学生到班级
     */
    @PostMapping("/{classId}/students")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Void> addStudentToClass(@AuthenticationPrincipal Long teacherId,
                                          @PathVariable Long classId,
                                          @RequestParam Long studentId) {
        classService.addStudentToClass(teacherId, classId, studentId);
        return Result.success("添加成功");
    }

    /**
     * 从班级移除学生
     */
    @DeleteMapping("/{classId}/students/{studentId}")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Void> removeStudentFromClass(@AuthenticationPrincipal Long teacherId,
                                               @PathVariable Long classId,
                                               @PathVariable Long studentId) {
        classService.removeStudentFromClass(teacherId, classId, studentId);
        return Result.success("移除成功");
    }

    /**
     * 学生加入班级
     */
    @PostMapping("/join")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Void> joinClass(@AuthenticationPrincipal Long studentId,
                                  @RequestParam String joinCode) {
        classService.joinClass(studentId, joinCode);
        return Result.success("加入成功");
    }

    /**
     * 学生退出班级
     */
    @PostMapping("/{classId}/leave")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Void> leaveClass(@AuthenticationPrincipal Long studentId,
                                   @PathVariable Long classId) {
        classService.leaveClass(studentId, classId);
        return Result.success("退出成功");
    }

    /**
     * 批量导入学生（教师）
     */
    @PostMapping("/{classId}/students/batch")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Void> batchImportStudents(@AuthenticationPrincipal Long teacherId,
                                            @PathVariable Long classId,
                                            @RequestBody List<Long> studentIds) {
        classService.batchImportStudents(teacherId, classId, studentIds);
        return Result.success("导入成功");
    }

    /**
     * 获取学生加入的班级列表
     */
    @GetMapping("/student/list")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<List<ClassVO>> getStudentClasses(@AuthenticationPrincipal Long studentId) {
        List<ClassVO> classes = classService.getStudentClasses(studentId);
        return Result.success(classes);

    }
    /**
     * 检查学生是否加入了任何班级
     */
    @GetMapping("/student/check-in-class")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Boolean> checkStudentInAnyClass(@AuthenticationPrincipal Long studentId) {
        boolean inClass = classService.isStudentInAnyClass(studentId);
        return Result.success(inClass);
    }
}